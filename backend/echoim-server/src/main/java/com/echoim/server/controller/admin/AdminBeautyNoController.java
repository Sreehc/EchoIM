package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequireLogin
@RestController
@RequestMapping("/admin/beauty-nos")
public class AdminBeautyNoController {

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(List.of(
                Map.of(
                        "beautyNoId", 1L,
                        "beautyNo", "88888",
                        "levelType", 3,
                        "status", 1,
                        "remark", "至尊靓号"
                )
        ));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody BeautyNoRequest request) {
        return ApiResponse.success(Map.of(
                "beautyNoId", 1L,
                "beautyNo", request.beautyNo(),
                "levelType", request.levelType()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return ApiResponse.success();
    }

    public record BeautyNoRequest(
            @NotBlank(message = "靓号不能为空") String beautyNo,
            @NotNull(message = "靓号等级不能为空") Integer levelType,
            String remark
    ) {
    }
}
