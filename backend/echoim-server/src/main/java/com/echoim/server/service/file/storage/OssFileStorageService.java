package com.echoim.server.service.file.storage;

import com.aliyun.oss.OSS;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.config.FileProperties;
import com.echoim.server.entity.ImFileEntity;
import com.echoim.server.service.file.FileStreamPayload;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.Instant;
import java.util.Date;

@Service
public class OssFileStorageService implements FileStorageService {

    private final FileProperties fileProperties;
    private final ObjectProvider<OSS> ossProvider;

    public OssFileStorageService(FileProperties fileProperties,
                                 ObjectProvider<OSS> ossProvider) {
        this.fileProperties = fileProperties;
        this.ossProvider = ossProvider;
    }

    @Override
    public String storageType() {
        return "oss";
    }

    @Override
    public void store(String bucketName, String objectKey, byte[] bytes) {
        try {
            requireOssClient().putObject(bucketName, objectKey, new java.io.ByteArrayInputStream(bytes));
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    @Override
    public FileStreamPayload open(ImFileEntity entity, String disposition) {
        try {
            return new FileStreamPayload(
                    entity.getFileName(),
                    entity.getContentType(),
                    entity.getFileSize(),
                    disposition,
                    requireOssClient().getObject(entity.getBucketName(), entity.getObjectKey()).getObjectContent()
            );
        } catch (Exception ex) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "文件不存在");
        }
    }

    @Override
    public String generateDownloadUrl(ImFileEntity entity, String disposition, int expiresInSeconds) {
        Instant expireAt = Instant.now().plusSeconds(expiresInSeconds);
        URL url = requireOssClient().generatePresignedUrl(entity.getBucketName(), entity.getObjectKey(), Date.from(expireAt));
        return url.toString();
    }

    @Override
    public String generatePublicUrl(ImFileEntity entity) {
        if (!StringUtils.hasText(fileProperties.getBaseUrl())) {
            return "/api/files/public/" + entity.getId();
        }
        String baseUrl = fileProperties.getBaseUrl().trim();
        String path = "/api/files/public/" + entity.getId();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + path : baseUrl + path;
    }

    private OSS requireOssClient() {
        OSS oss = ossProvider.getIfAvailable();
        if (oss == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "OSS 存储未配置");
        }
        return oss;
    }
}
