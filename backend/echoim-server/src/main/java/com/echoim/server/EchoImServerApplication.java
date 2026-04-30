package com.echoim.server;

import com.echoim.server.config.CallProperties;
import com.echoim.server.config.FileProperties;
import com.echoim.server.config.ImProperties;
import com.echoim.server.config.JwtProperties;
import com.echoim.server.config.AuthProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@MapperScan("com.echoim.server.mapper")
@EnableConfigurationProperties({JwtProperties.class, ImProperties.class, FileProperties.class, CallProperties.class, AuthProperties.class})
@SpringBootApplication
public class EchoImServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchoImServerApplication.class, args);
    }
}
