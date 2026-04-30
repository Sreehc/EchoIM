package com.echoim.server.service.file.storage;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.config.FileProperties;
import com.echoim.server.entity.ImFileEntity;
import com.echoim.server.service.file.FileStreamPayload;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final FileProperties fileProperties;
    private final HexFormat hexFormat = HexFormat.of();

    public LocalFileStorageService(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    @Override
    public String storageType() {
        return "local";
    }

    @Override
    public void store(String bucketName, String objectKey, byte[] bytes) {
        Path filePath = resolveStoragePath(objectKey);
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, bytes);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    @Override
    public FileStreamPayload open(ImFileEntity entity, String disposition) {
        Path filePath = resolveStoragePath(entity.getObjectKey());
        try {
            InputStream inputStream = Files.newInputStream(filePath);
            return new FileStreamPayload(
                    entity.getFileName(),
                    entity.getContentType(),
                    entity.getFileSize(),
                    disposition,
                    inputStream
            );
        } catch (IOException ex) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "文件不存在");
        }
    }

    @Override
    public String generateDownloadUrl(ImFileEntity entity, String disposition, int expiresInSeconds) {
        long expiresAt = Instant.now().plusSeconds(expiresInSeconds).toEpochMilli();
        String normalizedDisposition = normalizeDisposition(disposition);
        String signature = sign(entity.getId(), normalizedDisposition, expiresAt);
        return buildApiUrl("/api/files/access/" + entity.getId()
                + "?expires=" + expiresAt
                + "&disposition=" + normalizedDisposition
                + "&sig=" + signature);
    }

    @Override
    public String generatePublicUrl(ImFileEntity entity) {
        return buildApiUrl("/api/files/public/" + entity.getId());
    }

    public boolean verifySignature(Long fileId, String disposition, long expiresAt, String signature) {
        if (fileId == null || expiresAt < System.currentTimeMillis() || !StringUtils.hasText(signature)) {
            return false;
        }
        String expected = sign(fileId, normalizeDisposition(disposition), expiresAt);
        return expected.equals(signature);
    }

    private String sign(Long fileId, String disposition, long expiresAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(fileProperties.getAccessSignSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal((fileId + ":" + disposition + ":" + expiresAt).getBytes(StandardCharsets.UTF_8));
            return hexFormat.formatHex(signature);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "文件签名失败");
        }
    }

    private Path resolveStoragePath(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "文件不存在");
        }
        Path rootPath = Paths.get(fileProperties.getLocal().getRootDir()).toAbsolutePath().normalize();
        Path filePath = rootPath.resolve(objectKey).normalize();
        if (!filePath.startsWith(rootPath)) {
            throw new BizException(ErrorCode.FORBIDDEN, "文件路径非法");
        }
        return filePath;
    }

    private String normalizeDisposition(String disposition) {
        return "attachment".equalsIgnoreCase(disposition) ? "attachment" : "inline";
    }

    private String buildApiUrl(String path) {
        if (!StringUtils.hasText(fileProperties.getBaseUrl())) {
            return path;
        }
        String baseUrl = fileProperties.getBaseUrl().trim();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + path : baseUrl + path;
    }
}
