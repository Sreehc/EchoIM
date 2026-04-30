package com.echoim.server.service.file.storage;

import com.echoim.server.entity.ImFileEntity;
import com.echoim.server.service.file.FileStreamPayload;

public interface FileStorageService {

    String storageType();

    void store(String bucketName, String objectKey, byte[] bytes);

    FileStreamPayload open(ImFileEntity entity, String disposition);

    String generateDownloadUrl(ImFileEntity entity, String disposition, int expiresInSeconds);

    String generatePublicUrl(ImFileEntity entity);
}
