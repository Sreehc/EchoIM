package com.echoim.server.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "echoim.file.oss", name = "endpoint")
    public OSS ossClient(FileProperties fileProperties) {
        FileProperties.Oss oss = fileProperties.getOss();
        return new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret());
    }
}
