package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.success(Map.of(
                "userId", 10001L,
                "userNo", "E10001",
                "username", "echo_demo_01",
                "nickname", "Echo用户01",
                "avatarUrl", "",
                "signature", "欢迎来到 EchoIM"
        ));
    }

    @PutMapping("/me")
    public ApiResponse<Map<String, Object>> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nickname", request.nickname());
        data.put("avatarUrl", request.avatarUrl());
        data.put("gender", request.gender());
        data.put("signature", request.signature());
        return ApiResponse.success(data);
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<Map<String, Object>>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo 最小为 1") long pageNo,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "pageSize 最小为 1") long pageSize
    ) {
        List<Map<String, Object>> list = List.of(Map.of(
                "userId", 10002L,
                "userNo", "E10002",
                "nickname", "Echo用户02",
                "avatarUrl", "",
                "friendStatus", 0,
                "keyword", keyword
        ));
        return ApiResponse.success(new PageResponse<>(list, pageNo, pageSize, list.size()));
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "昵称不能为空") String nickname,
            String avatarUrl,
            Integer gender,
            String signature
    ) {
    }
}
