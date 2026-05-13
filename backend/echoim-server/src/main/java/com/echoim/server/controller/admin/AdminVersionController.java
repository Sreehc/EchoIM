package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireAdmin;
import com.echoim.server.common.annotation.RequireLogin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequireLogin
@RequireAdmin
@RestController
@RequestMapping({"/api/admin/versions", "/admin/versions"})
public class AdminVersionController {

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(List.of(
                Map.of(
                        "versionId", 1L,
                        "versionCode", "v0.1.0",
                        "versionName", "EchoIM MVP",
                        "platform", "web",
                        "releaseNote", "首个发布版本",
                        "forceUpdate", 0,
                        "grayPercent", 100,
                        "publishStatus", 1
                )
        ));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody VersionRequest request) {
        return ApiResponse.success(Map.of(
                "versionId", 1L,
                "versionCode", request.versionCode(),
                "versionName", request.versionName()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @Valid @RequestBody VersionRequest request) {
        return ApiResponse.success(Map.of(
                "versionId", id,
                "versionCode", request.versionCode(),
                "versionName", request.versionName()
        ));
    }

    public record VersionRequest(
            @NotBlank(message = "版本号不能为空") String versionCode,
            @NotBlank(message = "版本名称不能为空") String versionName,
            @NotBlank(message = "平台不能为空") String platform,
            String releaseNote,
            @NotNull(message = "是否强更不能为空") Integer forceUpdate,
            @NotNull(message = "灰度比例不能为空") Integer grayPercent
    ) {
    }
}
