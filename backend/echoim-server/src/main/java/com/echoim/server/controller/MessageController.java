package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.ratelimit.RateLimit;
import com.echoim.server.dto.message.EditMessageRequestDto;
import com.echoim.server.dto.message.ForwardMessageRequestDto;
import com.echoim.server.dto.message.ReactionMessageRequestDto;
import com.echoim.server.service.message.MessageCommandService;
import com.echoim.server.vo.conversation.MessageItemVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequireLogin
public class MessageController {

    private final MessageCommandService messageCommandService;

    public MessageController(MessageCommandService messageCommandService) {
        this.messageCommandService = messageCommandService;
    }

    @PutMapping("/{id}/recall")
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "message-recall", permits = 20, windowSeconds = 60, message = "撤回过于频繁")
    public ApiResponse<Map<String, Object>> recall(@PathVariable Long id) {
        return ApiResponse.success(messageCommandService.recall(LoginUserContext.requireUserId(), id));
    }

    @PutMapping("/{id}/edit")
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "message-edit", permits = 20, windowSeconds = 60, message = "编辑过于频繁")
    public ApiResponse<Map<String, Object>> edit(@PathVariable Long id,
                                                 @Valid @RequestBody EditMessageRequestDto requestDto) {
        return ApiResponse.success(messageCommandService.edit(LoginUserContext.requireUserId(), id, requestDto));
    }

    @PostMapping("/forward")
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "message-forward", permits = 20, windowSeconds = 60, message = "转发过于频繁")
    public ApiResponse<Map<String, Object>> forward(@Valid @RequestBody ForwardMessageRequestDto requestDto) {
        return ApiResponse.success(messageCommandService.forward(LoginUserContext.requireUserId(), requestDto));
    }

    @PutMapping("/{id}/reaction")
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "message-reaction", permits = 60, windowSeconds = 60, message = "操作过于频繁")
    public ApiResponse<MessageItemVo> toggleReaction(@PathVariable Long id,
                                                     @Valid @RequestBody ReactionMessageRequestDto requestDto) {
        return ApiResponse.success(messageCommandService.toggleReaction(LoginUserContext.requireUserId(), id, requestDto.getEmoji()));
    }

    @PutMapping("/{id}/pin")
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "message-pin", permits = 30, windowSeconds = 60, message = "操作过于频繁")
    public ApiResponse<MessageItemVo> pinMessage(@PathVariable Long id) {
        return ApiResponse.success(messageCommandService.pinMessage(LoginUserContext.requireUserId(), id));
    }

    @PutMapping("/{id}/unpin")
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "message-unpin", permits = 30, windowSeconds = 60, message = "操作过于频繁")
    public ApiResponse<MessageItemVo> unpinMessage(@PathVariable Long id) {
        return ApiResponse.success(messageCommandService.unpinMessage(LoginUserContext.requireUserId(), id));
    }

    @GetMapping("/pinned")
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "message-list-pinned", permits = 30, windowSeconds = 60, message = "查询过于频繁")
    public ApiResponse<List<MessageItemVo>> listPinnedMessages(@RequestParam Long conversationId) {
        return ApiResponse.success(messageCommandService.listPinnedMessages(LoginUserContext.requireUserId(), conversationId));
    }
}
