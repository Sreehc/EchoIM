package com.echoim.server.service.impl;

import com.echoim.server.common.audit.AuditLogService;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.config.FileProperties;
import com.echoim.server.entity.ImFileEntity;
import com.echoim.server.im.model.WsMessageItem;
import com.echoim.server.mapper.ImFileMapper;
import com.echoim.server.service.file.FileService;
import com.echoim.server.service.file.FileStreamPayload;
import com.echoim.server.service.file.storage.FileStorageService;
import com.echoim.server.service.file.storage.LocalFileStorageService;
import com.echoim.server.vo.conversation.MessageItemVo;
import com.echoim.server.vo.file.FileDownloadVo;
import com.echoim.server.vo.file.FileInfoVo;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl implements FileService {

    private static final int FILE_STATUS_NORMAL = 1;
    private static final int BIZ_TYPE_AVATAR = 1;
    private static final int BIZ_TYPE_IMAGE = 2;
    private static final int BIZ_TYPE_FILE = 4;
    private static final int BIZ_TYPE_AUDIO = 5;

    private final ImFileMapper imFileMapper;
    private final FileProperties fileProperties;
    private final AuditLogService auditLogService;
    private final LocalFileStorageService localFileStorageService;
    private final Map<String, FileStorageService> storageServices;
    private final Tika tika = new Tika();

    public FileServiceImpl(ImFileMapper imFileMapper,
                           FileProperties fileProperties,
                           AuditLogService auditLogService,
                           LocalFileStorageService localFileStorageService,
                           List<FileStorageService> storageServices) {
        this.imFileMapper = imFileMapper;
        this.fileProperties = fileProperties;
        this.auditLogService = auditLogService;
        this.localFileStorageService = localFileStorageService;
        this.storageServices = storageServices.stream()
                .collect(Collectors.toMap(FileStorageService::storageType, Function.identity()));
    }

    @Override
    public FileInfoVo upload(Long userId, MultipartFile file, Integer bizType) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > fileProperties.getMaxSize()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件大小超出限制");
        }

        int resolvedBizType = resolveBizType(file, bizType);
        String fileName = normalizeFileName(file.getOriginalFilename());
        String ext = extractExt(fileName);
        byte[] bytes = readBytes(file);
        String detectedContentType = detectContentType(bytes, fileName);
        validateFileSecurity(resolvedBizType, file.getContentType(), detectedContentType, ext, bytes);

        String objectKey = buildObjectKey(userId, ext);
        FileStorageService storageService = currentStorageService();
        storageService.store(resolveBucketName(storageService), objectKey, bytes);

        ImFileEntity entity = new ImFileEntity();
        entity.setOwnerUserId(userId);
        entity.setBizType(resolvedBizType);
        entity.setStorageType(storageService.storageType());
        entity.setBucketName(resolveBucketName(storageService));
        entity.setObjectKey(objectKey);
        entity.setFileName(fileName);
        entity.setFileExt(ext);
        entity.setContentType(defaultContentType(detectedContentType));
        entity.setFileSize(file.getSize());
        entity.setStatus(FILE_STATUS_NORMAL);
        imFileMapper.insert(entity);

        entity.setUrl(storageService.generatePublicUrl(entity));
        imFileMapper.updateById(entity);

        auditLogService.log("FILE_UPLOAD", Map.of(
                "userId", userId,
                "fileId", entity.getId(),
                "fileName", fileName,
                "bizType", resolvedBizType,
                "storageType", storageService.storageType()
        ));
        return toFileInfoVo(entity);
    }

    @Override
    public FileInfoVo getFileInfo(Long userId, Long fileId) {
        ImFileEntity entity = requireAccessibleFile(userId, fileId);
        auditLogService.log("FILE_INFO", Map.of("userId", userId, "fileId", fileId));
        return toFileInfoVo(entity);
    }

    @Override
    public FileDownloadVo getDownloadInfo(Long userId, Long fileId) {
        ImFileEntity entity = requireAccessibleFile(userId, fileId);
        FileDownloadVo vo = new FileDownloadVo();
        vo.setFileId(entity.getId());
        vo.setDownloadUrl(resolveDownloadUrl(entity));
        vo.setExpiresIn(fileProperties.getSignedUrlExpireSeconds());
        vo.setExpireAt(LocalDateTime.now().plusSeconds(fileProperties.getSignedUrlExpireSeconds()));
        auditLogService.log("FILE_DOWNLOAD", Map.of("userId", userId, "fileId", fileId));
        return vo;
    }

    @Override
    public FileStreamPayload getPublicFileStream(Long fileId) {
        ImFileEntity entity = requireNormalFile(fileId);
        if (!Integer.valueOf(BIZ_TYPE_AVATAR).equals(entity.getBizType())) {
            throw new BizException(ErrorCode.FORBIDDEN, "该文件不支持公开访问");
        }
        return storageServiceFor(entity).open(entity, "inline");
    }

    @Override
    public FileStreamPayload getSignedFileStream(Long fileId, long expiresAt, String disposition, String signature) {
        ImFileEntity entity = requireNormalFile(fileId);
        if (!localFileStorageService.verifySignature(fileId, disposition, expiresAt, signature)) {
            throw new BizException(ErrorCode.FORBIDDEN, "文件访问签名无效");
        }
        return storageServiceFor(entity).open(entity, normalizeDisposition(disposition, entity));
    }

    @Override
    public ImFileEntity requireOwnedFile(Long userId, Long fileId, Integer... allowedBizTypes) {
        ImFileEntity entity = requireNormalFile(fileId);
        if (!Objects.equals(entity.getOwnerUserId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权使用该文件");
        }
        if (allowedBizTypes != null && allowedBizTypes.length > 0) {
            List<Integer> allowed = Arrays.asList(allowedBizTypes);
            if (!allowed.contains(entity.getBizType())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "文件类型不匹配");
            }
        }
        return entity;
    }

    @Override
    public void enrichMessages(Long userId, List<MessageItemVo> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        List<Long> fileIds = messages.stream()
                .map(MessageItemVo::getFileId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (fileIds.isEmpty()) {
            return;
        }
        Map<Long, ImFileEntity> fileMap = loadFileMap(fileIds);
        for (MessageItemVo message : messages) {
            if (message.getFileId() == null) {
                continue;
            }
            ImFileEntity entity = fileMap.get(message.getFileId());
            if (entity != null && Objects.equals(entity.getStatus(), FILE_STATUS_NORMAL)) {
                message.setFile(toFileInfoVo(entity));
            }
        }
    }

    @Override
    public void enrichWsMessage(Long userId, WsMessageItem item) {
        if (item == null || item.getFileId() == null) {
            return;
        }
        ImFileEntity entity = imFileMapper.selectById(item.getFileId());
        if (entity == null || !Objects.equals(entity.getStatus(), FILE_STATUS_NORMAL)) {
            return;
        }
        item.setFile(toFileInfoVo(entity));
    }

    private ImFileEntity requireAccessibleFile(Long userId, Long fileId) {
        ImFileEntity entity = requireNormalFile(fileId);
        if (Objects.equals(entity.getOwnerUserId(), userId)) {
            return entity;
        }
        if (imFileMapper.countAccessibleByConversation(fileId, userId) == 0L) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该文件");
        }
        return entity;
    }

    private ImFileEntity requireNormalFile(Long fileId) {
        ImFileEntity entity = imFileMapper.selectById(fileId);
        if (entity == null || !Objects.equals(entity.getStatus(), FILE_STATUS_NORMAL)) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "文件不存在");
        }
        return entity;
    }

    private Map<Long, ImFileEntity> loadFileMap(List<Long> fileIds) {
        Map<Long, ImFileEntity> fileMap = new HashMap<>();
        for (ImFileEntity entity : imFileMapper.selectBatchIds(fileIds)) {
            fileMap.put(entity.getId(), entity);
        }
        return fileMap;
    }

    private FileInfoVo toFileInfoVo(ImFileEntity entity) {
        FileInfoVo vo = new FileInfoVo();
        vo.setFileId(entity.getId());
        vo.setFileName(entity.getFileName());
        vo.setFileExt(entity.getFileExt());
        vo.setContentType(entity.getContentType());
        vo.setFileSize(entity.getFileSize());
        vo.setBizType(entity.getBizType());
        vo.setObjectKey(entity.getObjectKey());
        vo.setUrl(storageServiceFor(entity).generatePublicUrl(entity));
        vo.setDownloadUrl(resolveDownloadUrl(entity));
        vo.setExpiresIn(fileProperties.getSignedUrlExpireSeconds());
        vo.setExpireAt(LocalDateTime.now().plusSeconds(fileProperties.getSignedUrlExpireSeconds()));
        return vo;
    }

    private String resolveDownloadUrl(ImFileEntity entity) {
        return storageServiceFor(entity).generateDownloadUrl(
                entity,
                normalizeDisposition(null, entity),
                fileProperties.getSignedUrlExpireSeconds()
        );
    }

    private FileStorageService currentStorageService() {
        return storageServices.computeIfAbsent(fileProperties.getStorageType(), ignored -> {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "不支持的文件存储类型");
        });
    }

    private FileStorageService storageServiceFor(ImFileEntity entity) {
        String storageType = StringUtils.hasText(entity.getStorageType()) ? entity.getStorageType() : fileProperties.getStorageType();
        FileStorageService storageService = storageServices.get(storageType);
        if (storageService == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "不支持的文件存储类型");
        }
        return storageService;
    }

    private String resolveBucketName(FileStorageService storageService) {
        return "oss".equals(storageService.storageType()) ? fileProperties.getOss().getBucketName() : null;
    }

    private int resolveBizType(MultipartFile file, Integer bizType) {
        if (bizType != null) {
            if (bizType != BIZ_TYPE_AVATAR && bizType != BIZ_TYPE_IMAGE && bizType != BIZ_TYPE_FILE && bizType != BIZ_TYPE_AUDIO) {
                throw new BizException(ErrorCode.PARAM_ERROR, "bizType 仅支持头像、图片、音频或文件");
            }
            return bizType;
        }
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            return BIZ_TYPE_IMAGE;
        }
        if (contentType != null && contentType.startsWith("audio/")) {
            return BIZ_TYPE_AUDIO;
        }
        return BIZ_TYPE_FILE;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "读取文件失败");
        }
    }

    private String detectContentType(byte[] bytes, String fileName) {
        try {
            return defaultContentType(tika.detect(bytes, fileName));
        } catch (Exception ex) {
            return "application/octet-stream";
        }
    }

    private void validateFileSecurity(int resolvedBizType,
                                      String clientContentType,
                                      String detectedContentType,
                                      String ext,
                                      byte[] bytes) {
        if (resolvedBizType == BIZ_TYPE_AVATAR || resolvedBizType == BIZ_TYPE_IMAGE) {
            validateWhitelist(ext, detectedContentType, fileProperties.getAllowedImageExtensions(), fileProperties.getAllowedImageContentTypes());
            if (clientContentType != null && !clientContentType.startsWith("image/")) {
                throw new BizException(ErrorCode.PARAM_ERROR, "图片类型不合法");
            }
            try {
                if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "图片内容不合法");
                }
            } catch (IOException ex) {
                throw new BizException(ErrorCode.PARAM_ERROR, "图片内容不合法");
            }
            return;
        }

        if (resolvedBizType == BIZ_TYPE_AUDIO) {
            validateWhitelist(ext, detectedContentType, fileProperties.getAllowedAudioExtensions(), fileProperties.getAllowedAudioContentTypes());
            if (clientContentType != null
                    && !"application/octet-stream".equals(clientContentType)
                    && !fileProperties.getAllowedAudioContentTypes().contains(clientContentType)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "音频类型不合法");
            }
            return;
        }

        validateWhitelist(ext, detectedContentType, fileProperties.getAllowedFileExtensions(), fileProperties.getAllowedFileContentTypes());
        if (clientContentType != null
                && !"application/octet-stream".equals(clientContentType)
                && !fileProperties.getAllowedFileContentTypes().contains(clientContentType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件类型不合法");
        }
    }

    private void validateWhitelist(String ext, String detectedContentType, List<String> extensions, List<String> contentTypes) {
        if (!StringUtils.hasText(ext) || extensions.stream().noneMatch(item -> item.equalsIgnoreCase(ext))) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件扩展名不合法");
        }
        if (!contentTypes.contains(detectedContentType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件内容类型不合法");
        }
    }

    private String normalizeFileName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "unknown";
        }
        return originalFilename.trim();
    }

    private String extractExt(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String buildObjectKey(Long userId, String ext) {
        LocalDateTime now = LocalDateTime.now();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String suffix = StringUtils.hasText(ext) ? "." + ext : "";
        return "echoim"
                + "/" + now.getYear()
                + "/" + String.format("%02d", now.getMonthValue())
                + "/" + String.format("%02d", now.getDayOfMonth())
                + "/" + userId
                + "/" + uuid
                + suffix;
    }

    private String normalizeDisposition(String disposition, ImFileEntity entity) {
        if (StringUtils.hasText(disposition)) {
            return "attachment".equalsIgnoreCase(disposition) ? "attachment" : "inline";
        }
        if (entity == null || entity.getContentType() == null) {
            return "attachment";
        }
        return entity.getContentType().startsWith("image/") ? "inline" : "attachment";
    }

    private String defaultContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }
}
