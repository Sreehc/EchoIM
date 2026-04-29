package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.service.search.GlobalSearchService;
import com.echoim.server.vo.search.GlobalSearchResponseVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequireLogin
public class SearchController {

    private final GlobalSearchService globalSearchService;

    public SearchController(GlobalSearchService globalSearchService) {
        this.globalSearchService = globalSearchService;
    }

    @GetMapping("/global")
    public ApiResponse<GlobalSearchResponseVo> global(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer conversationLimit,
            @RequestParam(required = false) Integer userLimit,
            @RequestParam(required = false) Integer messageLimit
    ) {
        return ApiResponse.success(globalSearchService.search(
                LoginUserContext.requireUserId(),
                keyword,
                conversationLimit,
                userLimit,
                messageLimit
        ));
    }
}
