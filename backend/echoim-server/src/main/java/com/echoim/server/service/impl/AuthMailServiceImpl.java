package com.echoim.server.service.impl;

import com.echoim.server.config.AuthProperties;
import com.echoim.server.service.auth.AuthMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthMailServiceImpl implements AuthMailService {

    private static final Logger log = LoggerFactory.getLogger(AuthMailServiceImpl.class);

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final AuthProperties authProperties;

    public AuthMailServiceImpl(JavaMailSender javaMailSender,
                               MailProperties mailProperties,
                               AuthProperties authProperties) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
        this.authProperties = authProperties;
    }

    @Override
    public void sendVerificationCode(String email, String sceneLabel, String code) {
        if (!StringUtils.hasText(mailProperties.getHost()) || !StringUtils.hasText(resolveFromAddress())) {
            log.warn("mail.disabled scene={} email={} code={}", sceneLabel, email, code);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(resolveFromAddress());
        message.setSubject("EchoIM 验证码");
        message.setText("""
                你正在进行 %s。

                验证码：%s

                10 分钟内有效。如非本人操作，请忽略此邮件。
                """.formatted(sceneLabel, code));
        javaMailSender.send(message);
    }

    private String resolveFromAddress() {
        if (StringUtils.hasText(authProperties.getMailFrom())) {
            return authProperties.getMailFrom();
        }
        return mailProperties.getUsername();
    }
}
