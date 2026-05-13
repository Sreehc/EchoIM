package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireAdmin;
import com.echoim.server.common.PageResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.entity.ImUserBanEntity;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.mapper.ImUserBanMapper;
import com.echoim.server.mapper.ImUserMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RequireLogin
@RequireAdmin
@RestController
@RequestMapping("/api/admin/bans")
public class AdminBanController {

    private final ImUserBanMapper banMapper;
    private final ImUserMapper userMapper;

    public AdminBanController(ImUserBanMapper banMapper, ImUserMapper userMapper) {
        this.banMapper = banMapper;
        this.userMapper = userMapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listBans(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        long offset = (pageNo - 1) * pageSize;
        var list = banMapper.selectBanPage(userId, offset, pageSize);
        long total = banMapper.countBans(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> banUser(@RequestBody Map<String, Object> request) {
        Long userId = request.get("userId") instanceof Number ? ((Number) request.get("userId")).longValue() : null;
        String reason = (String) request.get("reason");
        Integer banMinutes = (Integer) request.get("banMinutes");

        if (userId == null || reason == null || reason.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "参数不完整");
        }
        ImUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        ImUserBanEntity ban = new ImUserBanEntity();
        ban.setUserId(userId);
        ban.setReason(reason);
        ban.setBannedBy(LoginUserContext.requireUserId());
        ban.setStatus(1);

        if (banMinutes != null && banMinutes > 0) {
            ban.setBanType(1);
            ban.setBanMinutes(banMinutes);
            ban.setExpireAt(LocalDateTime.now().plusMinutes(banMinutes));
        } else {
            ban.setBanType(2);
        }

        banMapper.insert(ban);

        // Disable the user
        user.setStatus(2);
        userMapper.updateById(user);

        return ApiResponse.success(Map.of("banId", ban.getId(), "success", true));
    }

    @PutMapping("/{id}/unban")
    public ApiResponse<Void> unbanUser(@PathVariable Long id) {
        ImUserBanEntity ban = banMapper.selectById(id);
        if (ban == null) {
            return ApiResponse.success();
        }
        ban.setStatus(2);
        banMapper.updateById(ban);

        // Re-enable user if no other active bans
        long activeBans = banMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImUserBanEntity>()
                        .eq(ImUserBanEntity::getUserId, ban.getUserId())
                        .eq(ImUserBanEntity::getStatus, 1)
                        .ne(ImUserBanEntity::getId, id)
        ).size();
        if (activeBans == 0) {
            ImUserEntity user = userMapper.selectById(ban.getUserId());
            if (user != null && user.getStatus() == 2) {
                user.setStatus(1);
                userMapper.updateById(user);
            }
        }
        return ApiResponse.success();
    }
}
