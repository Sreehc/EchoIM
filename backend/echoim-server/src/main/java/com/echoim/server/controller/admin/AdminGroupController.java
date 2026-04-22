package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.PageResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/groups")
public class AdminGroupController {

    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> groups(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        List<Map<String, Object>> list = List.of(
                Map.of(
                        "groupId", 20001L,
                        "groupName", "Echo 项目讨论群",
                        "ownerUserId", 10001L,
                        "status", 1
                )
        );
        return ApiResponse.success(new PageResponse<>(list, pageNo, pageSize, list.size()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> dissolve(@PathVariable Long id) {
        return ApiResponse.success();
    }
}
