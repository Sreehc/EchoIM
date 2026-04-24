package com.echoim.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "echoim.file")
public class FileProperties {

    private String storageType = "oss";
    private long maxSize = 104857600L;
    private int signedUrlExpireSeconds = 600;
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
