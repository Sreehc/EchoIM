package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireAdmin;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.service.sensitive.SensitiveWordService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sensitive-words")
@RequireLogin
@RequireAdmin
public class AdminSensitiveWordController {

    private final SensitiveWordService sensitiveWordService;

    public AdminSensitiveWordController(SensitiveWordService sensitiveWordService) {
        this.sensitiveWordService = sensitiveWordService;
    }

    @GetMapping
    public ApiResponse<List<String>> listWords() {
        return ApiResponse.success(sensitiveWordService.getAllSensitiveWords());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> addWord(@RequestBody Map<String, Object> request) {
        String word = (String) request.get("word");
        String category = (String) request.getOrDefault("category", "default");
        Integer level = (Integer) request.getOrDefault("level", 1);
        Integer action = (Integer) request.getOrDefault("action", 1);
        Long userId = LoginUserContext.requireUserId();
        sensitiveWordService.addSensitiveWord(word, category, level, action, userId);
        return ApiResponse.success(Map.of("success", true));
    }

    @DeleteMapping("/{wordId}")
    public ApiResponse<Map<String, Object>> removeWord(@PathVariable Long wordId) {
        sensitiveWordService.removeSensitiveWord(wordId);
        return ApiResponse.success(Map.of("success", true));
    }

    @PostMapping("/reload")
    public ApiResponse<Map<String, Object>> reloadCache() {
        sensitiveWordService.reloadCache();
        return ApiResponse.success(Map.of("success", true));
    }
}
