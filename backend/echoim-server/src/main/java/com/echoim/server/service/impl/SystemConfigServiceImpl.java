package com.echoim.server.service.impl;

import com.echoim.server.mapper.SysConfigMapper;
import com.echoim.server.service.config.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SysConfigMapper sysConfigMapper;

    public SystemConfigServiceImpl(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    @Override
    public int getIntValue(String key, int defaultValue) {
        String value = sysConfigMapper.selectEnabledValueByKey(key);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
