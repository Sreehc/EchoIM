package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.PageResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.dto.conversation.ConversationPageQueryDto;
import com.echoim.server.dto.conversation.MessagePageQueryDto;
import com.echoim.server.service.conversation.ConversationService;
import com.echoim.server.vo.conversation.ConversationItemVo;
import com.echoim.server.vo.conversation.MessageItemVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @RequireLogin
    @GetMapping
    public ApiResponse<PageResponse<ConversationItemVo>> conversations(@Valid ConversationPageQueryDto queryDto) {
        return ApiResponse.success(conversationService.pageCurrentUserConversations(LoginUserContext.requireUserId(), queryDto));
    }

    @RequireLogin
    @GetMapping("/{id}/messages")
    public ApiResponse<PageResponse<MessageItemVo>> messages(@PathVariable Long id,
                                                             @Valid MessagePageQueryDto queryDto) {
        return ApiResponse.success(conversationService.pageConversationMessages(LoginUserContext.requireUserId(), id, queryDto));
    }

    @PutMapping("/{id}/top")
    public ApiResponse<Map<String, Object>> top(@PathVariable Long id,
                                                @Valid @RequestBody ConversationTopRequest request) {
        return ApiResponse.success(Map.of("conversationId", id, "isTop", request.isTop()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return ApiResponse.success();
    }

    @RequireLogin
    @PutMapping("/{id}/read")
    public ApiResponse<Map<String, Object>> read(@PathVariable Long id,
                                                 @Valid @RequestBody ConversationReadRequest request) {
        conversationService.readConversation(LoginUserContext.requireUserId(), id, request.lastReadSeq());
        return ApiResponse.success(Map.of("conversationId", id, "lastReadSeq", request.lastReadSeq()));
    }

    public record ConversationTopRequest(
            @NotNull(message = "置顶状态不能为空") Integer isTop
    ) {
    }

    public record ConversationReadRequest(
            @NotNull(message = "最后已读序号不能为空") Long lastReadSeq
    ) {
    }
}
