package com.echoim.server.service.impl;

import com.aliyun.oss.OSS;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.config.FileProperties;
import com.echoim.server.entity.ImFileEntity;
import com.echoim.server.im.model.WsMessageItem;
import com.echoim.server.mapper.ImFileMapper;
import com.echoim.server.service.file.FileService;
import com.echoim.server.vo.conversation.MessageItemVo;
import com.echoim.server.vo.file.FileDownloadVo;
import com.echoim.server.vo.file.FileInfoVo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class FileServiceImpl implements FileService {

    private static final int FILE_STATUS_NORMAL = 1;
    private static final int BIZ_TYPE_IMAGE = 2;
    private static final int BIZ_TYPE_FILE = 4;

    private final ImFileMapper imFileMapper;
    private final FileProperties fileProperties;
    private final ObjectProvider<OSS> ossProvider;

    public FileServiceImpl(ImFileMapper imFileMapper,
                           FileProperties fileProperties,
                           ObjectProvider<OSS> ossProvider) {
        this.imFileMapper = imFileMapper;
        this.fileProperties = fileProperties;
        this.ossProvider = ossProvider;
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
        OSS oss = requireOssClient();
        String fileName = normalizeFileName(file.getOriginalFilename());
        String ext = extractExt(fileName);
        String objectKey = buildObjectKey(userId, ext);
        try {
            oss.putObject(fileProperties.getOss().getBucketName(), objectKey, file.getInputStream());
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }

        ImFileEntity entity = new ImFileEntity();
        entity.setOwnerUserId(userId);
        entity.setBizType(resolvedBizType);
        entity.setStorageType(fileProperties.getStorageType());
        entity.setBucketName(fileProperties.getOss().getBucketName());
        entity.setObjectKey(objectKey);
        entity.setFileName(fileName);
        entity.setFileExt(ext);
        entity.setContentType(defaultContentType(file.getContentType()));
        entity.setFileSize(file.getSize());
        entity.setUrl(buildCanonicalUrl(objectKey));
        entity.setStatus(FILE_STATUS_NORMAL);
        imFileMapper.insert(entity);
        return toFileInfoVo(entity);
    }

    @Override
    public FileInfoVo getFileInfo(Long userId, Long fileId) {
        ImFileEntity entity = requireAccessibleFile(userId, fileId);
        return toFileInfoVo(entity);
    }

    @Override
    public FileDownloadVo getDownloadInfo(Long userId, Long fileId) {
        ImFileEntity entity = requireAccessibleFile(userId, fileId);
        FileDownloadVo vo = new FileDownloadVo();
        vo.setFileId(entity.getId());
        vo.setDownloadUrl(generateSignedUrl(entity.getObjectKey()));
        vo.setExpiresIn(fileProperties.getSignedUrlExpireSeconds());
        vo.setExpireAt(LocalDateTime.now().plusSeconds(fileProperties.getSignedUrlExpireSeconds()));
        return vo;
    }

    @Override
    public ImFileEntity requireOwnedFile(Long userId, Long fileId, Integer... allowedBizTypes) {
        ImFileEntity entity = imFileMapper.selectById(fileId);
        if (entity == null || !Objects.equals(entity.getStatus(), FILE_STATUS_NORMAL)) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "文件不存在");
        }
        if (!Objects.equals(entity.getOwnerUserId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权使用该文件");
        }
        if (allowedBizTypes != null && allowedBizTypes.length > 0) {
            Set<Integer> allowedSet = new HashSet<>(Arrays.asList(allowedBizTypes));
            if (!allowedSet.contains(entity.getBizType())) {
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
        ImFileEntity entity = imFileMapper.selectById(fileId);
        if (entity == null || !Objects.equals(entity.getStatus(), FILE_STATUS_NORMAL)) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "文件不存在");
        }
        if (Objects.equals(entity.getOwnerUserId(), userId)) {
            return entity;
        }
        if (imFileMapper.countAccessibleByConversation(fileId, userId) == 0L) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该文件");
        }
        return entity;
    }

    private Map<Long, ImFileEntity> loadFileMap(List<Long> fileIds) {
        List<ImFileEntity> entities = imFileMapper.selectBatchIds(fileIds);
        Map<Long, ImFileEntity> fileMap = new HashMap<>();
        for (ImFileEntity entity : entities) {
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
        vo.setDownloadUrl(generateSignedUrl(entity.getObjectKey()));
        vo.setExpiresIn(fileProperties.getSignedUrlExpireSeconds());
        vo.setExpireAt(LocalDateTime.now().plusSeconds(fileProperties.getSignedUrlExpireSeconds()));
        return vo;
    }

    private String generateSignedUrl(String objectKey) {
        OSS oss = requireOssClient();
        Instant expireAt = Instant.now().plusSeconds(fileProperties.getSignedUrlExpireSeconds());
        URL url = oss.generatePresignedUrl(fileProperties.getOss().getBucketName(), objectKey, Date.from(expireAt));
        return url.toString();
    }

    private OSS requireOssClient() {
        OSS oss = ossProvider.getIfAvailable();
        if (oss == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "OSS 存储未配置");
        }
        return oss;
    }

    private int resolveBizType(MultipartFile file, Integer bizType) {
        if (bizType != null) {
            if (bizType != BIZ_TYPE_IMAGE && bizType != BIZ_TYPE_FILE) {
                throw new BizException(ErrorCode.PARAM_ERROR, "bizType 仅支持图片或文件");
            }
            return bizType;
        }
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/") ? BIZ_TYPE_IMAGE : BIZ_TYPE_FILE;
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
        return fileProperties.getOss().getObjectPrefix()
                + "/" + now.getYear()
                + "/" + String.format("%02d", now.getMonthValue())
                + "/" + String.format("%02d", now.getDayOfMonth())
                + "/" + userId
                + "/" + uuid
                + suffix;
    }

    private String buildCanonicalUrl(String objectKey) {
        String endpoint = fileProperties.getOss().getEndpoint();
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(fileProperties.getOss().getBucketName())) {
            return null;
        }
        String cleanEndpoint = endpoint.replaceFirst("^https?://", "");
        return "https://" + fileProperties.getOss().getBucketName() + "." + cleanEndpoint + "/" + objectKey;
    }

    private String defaultContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }
}
