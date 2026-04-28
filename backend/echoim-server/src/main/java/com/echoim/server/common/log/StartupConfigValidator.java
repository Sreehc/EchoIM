package com.echoim.server.common.log;

import com.echoim.server.common.exception.BizException;
import com.echoim.server.config.FileProperties;
import com.echoim.server.config.JwtProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StartupConfigValidator implements ApplicationRunner {

    private final JwtProperties jwtProperties;
    private final FileProperties fileProperties;

    public StartupConfigValidator(JwtProperties jwtProperties, FileProperties fileProperties) {
        this.jwtProperties = jwtProperties;
        this.fileProperties = fileProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(jwtProperties.getSecret())
                || "your-jwt-secret".equals(jwtProperties.getSecret())) {
            throw new BizException(50000, "JWT secret 未配置");
        }
        if ("oss".equalsIgnoreCase(fileProperties.getStorageType())) {
            FileProperties.Oss oss = fileProperties.getOss();
            if (!StringUtils.hasText(oss.getEndpoint())
                    || !StringUtils.hasText(oss.getBucketName())
                    || !StringUtils.hasText(oss.getAccessKeyId())
                    || !StringUtils.hasText(oss.getAccessKeySecret())
                    || oss.getAccessKeyId().contains("your-access-key")
                    || oss.getAccessKeySecret().contains("your-access-key")) {
                throw new BizException(50000, "OSS 配置未完成");
            }
        }
    }
}
