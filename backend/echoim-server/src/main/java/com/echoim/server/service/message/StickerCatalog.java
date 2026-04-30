package com.echoim.server.service.message;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.vo.message.StickerPayloadVo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StickerCatalog {

    private final Map<String, StickerPayloadVo> stickersById = new LinkedHashMap<>();

    public StickerCatalog() {
        register("orbit_note", "Orbit Note");
        register("soft_signal", "Soft Signal");
        register("midnight_ping", "Midnight Ping");
    }

    public StickerPayloadVo require(String stickerId) {
        if (!StringUtils.hasText(stickerId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "贴纸参数错误");
        }
        StickerPayloadVo sticker = stickersById.get(stickerId.trim());
        if (sticker == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "贴纸不存在");
        }
        StickerPayloadVo normalized = new StickerPayloadVo();
        normalized.setStickerId(sticker.getStickerId());
        normalized.setTitle(sticker.getTitle());
        return normalized;
    }

    private void register(String stickerId, String title) {
        StickerPayloadVo sticker = new StickerPayloadVo();
        sticker.setStickerId(stickerId);
        sticker.setTitle(title);
        stickersById.put(stickerId, sticker);
    }
}
