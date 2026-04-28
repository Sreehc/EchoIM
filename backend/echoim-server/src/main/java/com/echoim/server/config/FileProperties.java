package com.echoim.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "echoim.file")
public class FileProperties {

    private String storageType = "oss";
    private long maxSize = 104857600L;
    private int signedUrlExpireSeconds = 600;
    private List<String> allowedImageContentTypes = new ArrayList<>(List.of("image/jpeg", "image/png", "image/gif", "image/webp"));
    private List<String> allowedImageExtensions = new ArrayList<>(List.of("jpg", "jpeg", "png", "gif", "webp"));
    private List<String> allowedFileContentTypes = new ArrayList<>(List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "application/zip",
            "application/x-zip-compressed"
    ));
    private List<String> allowedFileExtensions = new ArrayList<>(List.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip"
    ));
    private Oss oss = new Oss();

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public int getSignedUrlExpireSeconds() {
        return signedUrlExpireSeconds;
    }

    public void setSignedUrlExpireSeconds(int signedUrlExpireSeconds) {
        this.signedUrlExpireSeconds = signedUrlExpireSeconds;
    }

    public List<String> getAllowedImageContentTypes() {
        return allowedImageContentTypes;
    }

    public void setAllowedImageContentTypes(List<String> allowedImageContentTypes) {
        this.allowedImageContentTypes = allowedImageContentTypes;
    }

    public List<String> getAllowedImageExtensions() {
        return allowedImageExtensions;
    }

    public void setAllowedImageExtensions(List<String> allowedImageExtensions) {
        this.allowedImageExtensions = allowedImageExtensions;
    }

    public List<String> getAllowedFileContentTypes() {
        return allowedFileContentTypes;
    }

    public void setAllowedFileContentTypes(List<String> allowedFileContentTypes) {
        this.allowedFileContentTypes = allowedFileContentTypes;
    }

    public List<String> getAllowedFileExtensions() {
        return allowedFileExtensions;
    }

    public void setAllowedFileExtensions(List<String> allowedFileExtensions) {
        this.allowedFileExtensions = allowedFileExtensions;
    }

    public Oss getOss() {
        return oss;
    }

    public void setOss(Oss oss) {
        this.oss = oss;
    }

    public static class Oss {

        private String endpoint;
        private String bucketName;
        private String accessKeyId;
        private String accessKeySecret;
        private String objectPrefix = "echoim";

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getObjectPrefix() {
            return objectPrefix;
        }

        public void setObjectPrefix(String objectPrefix) {
            this.objectPrefix = objectPrefix;
        }
    }
}
