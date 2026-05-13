package com.echoim.server.common.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echoim.server.config.AdminProperties;
import com.echoim.server.entity.SysAdminUserEntity;
import com.echoim.server.mapper.SysAdminUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminProperties adminProperties;
    private final SysAdminUserMapper sysAdminUserMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(AdminProperties adminProperties,
                                SysAdminUserMapper sysAdminUserMapper,
                                PasswordEncoder passwordEncoder) {
        this.adminProperties = adminProperties;
        this.sysAdminUserMapper = sysAdminUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminProperties.getSuperUsername())
                || !StringUtils.hasText(adminProperties.getSuperPassword())) {
            log.warn("Super admin bootstrap skipped because username/password is not configured");
            return;
        }

        String username = adminProperties.getSuperUsername().trim();
        SysAdminUserEntity adminUser = sysAdminUserMapper.selectOne(new LambdaQueryWrapper<SysAdminUserEntity>()
                .eq(SysAdminUserEntity::getUsername, username)
                .last("LIMIT 1"));
        if (adminUser == null) {
            adminUser = new SysAdminUserEntity();
            adminUser.setUsername(username);
        }

        adminUser.setPasswordHash(passwordEncoder.encode(adminProperties.getSuperPassword()));
        adminUser.setNickname(StringUtils.hasText(adminProperties.getSuperNickname())
                ? adminProperties.getSuperNickname().trim()
                : "系统管理员");
        adminUser.setRoleCode("super_admin");
        adminUser.setStatus(1);

        if (adminUser.getId() == null) {
            sysAdminUserMapper.insert(adminUser);
            log.info("Bootstrapped super admin account: {}", username);
        } else {
            sysAdminUserMapper.updateById(adminUser);
            log.info("Updated super admin account: {}", username);
        }
    }
}
