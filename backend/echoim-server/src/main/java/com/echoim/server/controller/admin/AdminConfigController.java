package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequireLogin
@RestController
@RequestMapping({"/api/admin/configs", "/admin/configs"})
public class AdminConfigController {

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(List.of(
                Map.of(
                        "configId", 1L,
                        "configKey", "file.max-size-mb",
                        "configValue", "50",
                        "configName", "文件上传大小限制",
                        "remark", "超过此大小的文件将被拒绝上传",
                        "status", 1
                )
        ));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody ConfigRequest request) {
        return ApiResponse.success(Map.of(
                "configId", 1L,
                "configKey", request.configKey(),
                "configValue", request.configValue()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @Valid @RequestBody ConfigRequest request) {
        return ApiResponse.success(Map.of(
                "configId", id,
                "configKey", request.configKey(),
                "configValue", request.configValue()
        ));
    }

    public record ConfigRequest(
            @NotBlank(message = "配置键不能为空") String configKey,
            @NotBlank(message = "配置值不能为空") String configValue,
            @NotBlank(message = "配置名称不能为空") String configName,
            String remark
    ) {
    }
}
