package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.PageResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.dto.conversation.ConversationPageQueryDto;
import com.echoim.server.dto.conversation.MessagePageQueryDto;
import com.echoim.server.service.conversation.ConversationService;
import com.echoim.server.vo.conversation.ConversationFileVo;
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
    @PostMapping("/single")
    public ApiResponse<ConversationItemVo> createSingle(@Valid @RequestBody ConversationSingleCreateRequest request) {
        return ApiResponse.success(conversationService.createSingleConversation(LoginUserContext.requireUserId(), request.targetUserId()));
    }

    @RequireLogin
    @PostMapping("/saved")
    public ApiResponse<ConversationItemVo> createSaved() {
        return ApiResponse.success(conversationService.createSavedConversation(LoginUserContext.requireUserId()));
    }

    @RequireLogin
    @GetMapping("/{id}/messages")
    public ApiResponse<PageResponse<MessageItemVo>> messages(@PathVariable Long id,
                                                             @Valid MessagePageQueryDto queryDto) {
        return ApiResponse.success(conversationService.pageConversationMessages(LoginUserContext.requireUserId(), id, queryDto));
    }

    @RequireLogin
    @GetMapping("/{id}/files")
    public ApiResponse<PageResponse<ConversationFileVo>> files(@PathVariable Long id,
                                                               @RequestParam(defaultValue = "1") long pageNo,
                                                               @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(conversationService.pageConversationFiles(LoginUserContext.requireUserId(), id, pageNo, pageSize));
    }

    @RequireLogin
    @PutMapping("/{id}/top")
    public ApiResponse<Map<String, Object>> top(@PathVariable Long id,
                                                @Valid @RequestBody ConversationTopRequest request) {
        conversationService.updateTop(LoginUserContext.requireUserId(), id, request.isTop());
        return ApiResponse.success(Map.of("conversationId", id, "isTop", request.isTop()));
    }

    @RequireLogin
    @PutMapping("/{id}/mute")
    public ApiResponse<Map<String, Object>> mute(@PathVariable Long id,
                                                 @Valid @RequestBody ConversationMuteRequest request) {
        conversationService.updateMute(LoginUserContext.requireUserId(), id, request.isMute());
        return ApiResponse.success(Map.of("conversationId", id, "isMute", request.isMute()));
    }

    @RequireLogin
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        conversationService.deleteConversation(LoginUserContext.requireUserId(), id);
        return ApiResponse.success();
    }

    @RequireLogin
    @PutMapping("/{id}/read")
    public ApiResponse<Map<String, Object>> read(@PathVariable Long id,
                                                 @Valid @RequestBody ConversationReadRequest request) {
        conversationService.readConversation(LoginUserContext.requireUserId(), id, request.lastReadSeq());
        return ApiResponse.success(Map.of("conversationId", id, "lastReadSeq", request.lastReadSeq()));
    }

    @RequireLogin
    @PutMapping("/{id}/archive")
    public ApiResponse<Map<String, Object>> archive(@PathVariable Long id,
                                                    @Valid @RequestBody ConversationArchiveRequest request) {
        conversationService.updateArchive(LoginUserContext.requireUserId(), id, request.archived());
        return ApiResponse.success(Map.of("conversationId", id, "archived", request.archived()));
    }

    @RequireLogin
    @PutMapping("/{id}/unread")
    public ApiResponse<Map<String, Object>> unread(@PathVariable Long id,
                                                   @Valid @RequestBody ConversationUnreadRequest request) {
        conversationService.markConversationUnread(LoginUserContext.requireUserId(), id, request.unread());
        return ApiResponse.success(Map.of("conversationId", id, "unread", request.unread()));
    }

    @RequireLogin
    @PutMapping("/{id}/draft")
    public ApiResponse<Map<String, Object>> saveDraft(@PathVariable Long id,
                                                      @Valid @RequestBody ConversationDraftRequest request) {
        conversationService.saveDraft(LoginUserContext.requireUserId(), id, request.draftContent());
        return ApiResponse.success(Map.of("conversationId", id));
    }

    @RequireLogin
    @GetMapping("/{id}/draft")
    public ApiResponse<Map<String, Object>> loadDraft(@PathVariable Long id) {
        String draftContent = conversationService.loadDraft(LoginUserContext.requireUserId(), id);
        return ApiResponse.success(Map.of("conversationId", id, "draftContent", draftContent != null ? draftContent : ""));
    }

    public record ConversationTopRequest(
            @NotNull(message = "置顶状态不能为空") Integer isTop
    ) {
    }

    public record ConversationMuteRequest(
            @NotNull(message = "免打扰状态不能为空") Integer isMute
    ) {
    }

    public record ConversationReadRequest(
            @NotNull(message = "最后已读序号不能为空") Long lastReadSeq
    ) {
    }

    public record ConversationArchiveRequest(
            @NotNull(message = "归档状态不能为空") Boolean archived
    ) {
    }

    public record ConversationUnreadRequest(
            @NotNull(message = "未读状态不能为空") Boolean unread
    ) {
    }

    public record ConversationSingleCreateRequest(
            @NotNull(message = "目标用户不能为空") Long targetUserId
    ) {
    }

    public record ConversationDraftRequest(
            String draftContent
    ) {
    }
}
