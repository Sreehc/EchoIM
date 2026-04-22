package com.echoim.server;

import com.echoim.server.config.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@MapperScan("com.echoim.server.mapper")
@EnableConfigurationProperties(JwtProperties.class)
@SpringBootApplication
public class EchoImServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchoImServerApplication.class, args);
    }
}
