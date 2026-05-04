package com.echoim.server.service.impl;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.common.util.IdGenerator;
import com.echoim.server.dto.group.AddGroupMembersRequestDto;
import com.echoim.server.dto.group.CreateGroupRequestDto;
import com.echoim.server.dto.group.CreateInviteLinkRequestDto;
import com.echoim.server.dto.group.MuteMemberRequestDto;
import com.echoim.server.dto.group.ReviewJoinRequestDto;
import com.echoim.server.dto.group.UpdateGroupMemberRoleRequestDto;
import com.echoim.server.dto.group.UpdateGroupRequestDto;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.entity.ImGroupEntity;
import com.echoim.server.entity.ImGroupInviteEntity;
import com.echoim.server.entity.ImGroupJoinRequestEntity;
import com.echoim.server.entity.ImGroupMemberEntity;
import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImGroupInviteMapper;
import com.echoim.server.mapper.ImGroupJoinRequestMapper;
import com.echoim.server.mapper.ImGroupMapper;
import com.echoim.server.mapper.ImGroupMemberMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.mapper.ImUserMapper;
import com.echoim.server.service.group.GroupService;
import com.echoim.server.vo.group.GroupCreateVo;
import com.echoim.server.vo.group.GroupDetailVo;
import com.echoim.server.vo.group.GroupInviteItemVo;
import com.echoim.server.vo.group.GroupInviteLinkVo;
import com.echoim.server.vo.group.GroupJoinRequestItemVo;
import com.echoim.server.vo.group.GroupMemberItemVo;
import com.echoim.server.vo.group.InvitePreviewVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class GroupServiceImpl implements GroupService {

    private static final int GROUP_STATUS_NORMAL = 1;
    private static final int GROUP_STATUS_DISSOLVED = 2;
    private static final int MEMBER_ROLE_OWNER = 1;
    private static final int MEMBER_ROLE_MEMBER = 2;
    private static final int MEMBER_ROLE_ADMIN = 3;
    private static final int MEMBER_STATUS_NORMAL = 1;
    private static final int MEMBER_STATUS_LEFT = 2;
    private static final int MEMBER_STATUS_REMOVED = 3;
    private static final int JOIN_SOURCE_CREATE = 1;
    private static final int JOIN_SOURCE_INVITE = 2;
    private static final int CONVERSATION_TYPE_GROUP = 2;
    private static final int CONVERSATION_TYPE_CHANNEL = 3;
    private static final int CONVERSATION_STATUS_NORMAL = 1;
    private static final int INVITE_STATUS_ACTIVE = 1;
    private static final int INVITE_STATUS_REVOKED = 2;
    private static final int JOIN_REQUEST_PENDING = 0;
    private static final int JOIN_REQUEST_APPROVED = 1;
    private static final int JOIN_REQUEST_REJECTED = 2;
    private static final int JOIN_SOURCE_INVITE_LINK = 3;
    private static final int JOIN_SOURCE_JOIN_REQUEST = 4;

    private final ImGroupMapper imGroupMapper;
    private final ImGroupMemberMapper imGroupMemberMapper;
    private final ImConversationMapper imConversationMapper;
    private final ImConversationUserMapper imConversationUserMapper;
    private final ImMessageMapper imMessageMapper;
    private final ImUserMapper imUserMapper;
    private final ImWsPushService imWsPushService;
    private final ImGroupInviteMapper imGroupInviteMapper;
    private final ImGroupJoinRequestMapper imGroupJoinRequestMapper;

    public GroupServiceImpl(ImGroupMapper imGroupMapper,
                            ImGroupMemberMapper imGroupMemberMapper,
                            ImConversationMapper imConversationMapper,
                            ImConversationUserMapper imConversationUserMapper,
                            ImMessageMapper imMessageMapper,
                            ImUserMapper imUserMapper,
                            ImWsPushService imWsPushService,
                            ImGroupInviteMapper imGroupInviteMapper,
                            ImGroupJoinRequestMapper imGroupJoinRequestMapper) {
        this.imGroupMapper = imGroupMapper;
        this.imGroupMemberMapper = imGroupMemberMapper;
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imMessageMapper = imMessageMapper;
        this.imUserMapper = imUserMapper;
        this.imWsPushService = imWsPushService;
        this.imGroupInviteMapper = imGroupInviteMapper;
        this.imGroupJoinRequestMapper = imGroupJoinRequestMapper;
    }

    @Override
    @Transactional
    public GroupCreateVo createGroup(Long currentUserId, CreateGroupRequestDto requestDto) {
        Set<Long> memberIds = normalizeMemberIds(currentUserId, requestDto.getMemberIds());
        validateUsersExist(memberIds);
        int conversationType = normalizeConversationType(requestDto.getConversationType());

        ImGroupEntity group = new ImGroupEntity();
        group.setGroupNo(conversationType == CONVERSATION_TYPE_CHANNEL
                ? IdGenerator.channelNo() : IdGenerator.groupNo());
        group.setGroupName(requestDto.getGroupName().trim());
        group.setOwnerUserId(currentUserId);
        group.setConversationType(conversationType);
        group.setStatus(GROUP_STATUS_NORMAL);
        imGroupMapper.insert(group);

        for (Long memberId : memberIds) {
            int role = currentUserId.equals(memberId) ? MEMBER_ROLE_OWNER : MEMBER_ROLE_MEMBER;
            ensureGroupMember(group.getId(), memberId, role, currentUserId.equals(memberId) ? JOIN_SOURCE_CREATE : JOIN_SOURCE_INVITE);
        }

        ImConversationEntity conversation = ensureCollectiveConversation(group);
        for (Long memberId : memberIds) {
            ensureConversationUser(conversation.getId(), memberId, false);
        }
        pushConversationCreated(conversation.getId(), memberIds);

        GroupCreateVo vo = new GroupCreateVo();
        vo.setGroupId(group.getId());
        vo.setGroupNo(group.getGroupNo());
        vo.setGroupName(group.getGroupName());
        vo.setConversationId(conversation.getId());
        vo.setConversationNo(conversation.getConversationNo());
        vo.setMemberCount(memberIds.size());
        vo.setConversationType(conversationType);
        return vo;
    }

    @Override
    public GroupDetailVo getGroupDetail(Long currentUserId, Long groupId) {
        GroupDetailVo detail = imGroupMapper.selectGroupDetail(groupId, currentUserId);
        if (detail == null) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "群组不存在");
        }
        return detail;
    }

    @Override
    public List<GroupMemberItemVo> listMembers(Long currentUserId, Long groupId) {
        requireNormalGroup(groupId);
        requireActiveMember(groupId, currentUserId);
        return imGroupMemberMapper.selectActiveMemberItemsByGroupId(groupId);
    }

    @Override
    @Transactional
    public GroupDetailVo updateGroup(Long currentUserId, Long groupId, UpdateGroupRequestDto requestDto) {
        ImGroupEntity group = requireNormalGroup(groupId);
        ImGroupMemberEntity operator = requireActiveMember(groupId, currentUserId);
        if (!canEditMeta(operator)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权编辑群资料");
        }

        boolean touched = false;
        boolean noticeChanged = false;
        if (StringUtils.hasText(requestDto.getGroupName())) {
            group.setGroupName(requestDto.getGroupName().trim());
            touched = true;
        }
        if (requestDto.getNotice() != null) {
            String newNotice = requestDto.getNotice().trim();
            String oldNotice = group.getNotice();
            if (!newNotice.equals(oldNotice == null ? "" : oldNotice)) {
                group.setNotice(newNotice);
                noticeChanged = true;
            }
            touched = true;
        }
        if (!touched) {
            throw new BizException(ErrorCode.PARAM_ERROR, "没有可更新的内容");
        }

        imGroupMapper.updateById(group);
        ImConversationEntity conversation = ensureCollectiveConversation(group);

        if (noticeChanged) {
            String noticeText = group.getNotice().isEmpty()
                    ? String.format("%s 清除了群公告", operator.getNickName() != null ? operator.getNickName() : "管理员")
                    : String.format("%s 更新了群公告：%s", operator.getNickName() != null ? operator.getNickName() : "管理员", group.getNotice());
            pushSystemMessage(conversation.getId(), groupId, noticeText);
        }

        return getGroupDetail(currentUserId, groupId);
    }

    @Override
    @Transactional
    public void updateMemberRole(Long currentUserId, Long groupId, Long userId, UpdateGroupMemberRoleRequestDto requestDto) {
        ImGroupEntity group = requireNormalGroup(groupId);
        ImGroupMemberEntity operator = requireActiveMember(groupId, currentUserId);
        if (!Integer.valueOf(MEMBER_ROLE_OWNER).equals(operator.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有群主可以修改成员角色");
        }

        ImGroupMemberEntity target = requireActiveMember(groupId, userId);
        if (group.getOwnerUserId().equals(userId)) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "不能修改群主角色");
        }

        target.setRole(normalizeEditableRole(requestDto.getRole()));
        target.setUpdatedAt(LocalDateTime.now());
        imGroupMemberMapper.updateById(target);
    }

    @Override
    @Transactional
    public GroupDetailVo addMembers(Long currentUserId, Long groupId, AddGroupMembersRequestDto requestDto) {
        requireNormalGroup(groupId);
        ImGroupMemberEntity operator = requireActiveMember(groupId, currentUserId);
        if (!canManageMembers(operator)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权添加成员");
        }
        Set<Long> memberIds = normalizeMemberIds(null, requestDto.getMemberIds());
        validateUsersExist(memberIds);
        for (Long memberId : memberIds) {
            ensureGroupMember(groupId, memberId, MEMBER_ROLE_MEMBER, JOIN_SOURCE_INVITE);
        }
        ImConversationEntity conversation = ensureCollectiveConversation(requireNormalGroup(groupId));
        for (Long memberId : memberIds) {
            ensureConversationUser(conversation.getId(), memberId, false);
        }
        pushConversationCreated(conversation.getId(), memberIds);
        return getGroupDetail(currentUserId, groupId);
    }

    @Override
    @Transactional
    public void removeMember(Long currentUserId, Long groupId, Long userId) {
        ImGroupEntity group = requireNormalGroup(groupId);
        ImGroupMemberEntity operator = requireActiveMember(groupId, currentUserId);
        ImGroupMemberEntity target = requireActiveMember(groupId, userId);
        if (group.getOwnerUserId().equals(userId)) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "不能移除群主");
        }
        if (!canRemove(operator, target)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权移除该成员");
        }
        target.setStatus(MEMBER_STATUS_REMOVED);
        target.setUpdatedAt(LocalDateTime.now());
        imGroupMemberMapper.updateById(target);
        freezeConversationAtCurrentSeq(groupId, userId, true);
    }

    @Override
    @Transactional
    public void leaveGroup(Long currentUserId, Long groupId, boolean keepConversation) {
        ImGroupEntity group = requireNormalGroup(groupId);
        if (group.getOwnerUserId().equals(currentUserId)) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "群主不能直接退群");
        }
        ImGroupMemberEntity member = requireActiveMember(groupId, currentUserId);
        member.setStatus(MEMBER_STATUS_LEFT);
        member.setUpdatedAt(LocalDateTime.now());
        imGroupMemberMapper.updateById(member);
        freezeConversationAtCurrentSeq(groupId, currentUserId, keepConversation);
    }

    @Override
    @Transactional
    public void dissolveGroup(Long currentUserId, Long groupId) {
        ImGroupEntity group = requireNormalGroup(groupId);
        if (!group.getOwnerUserId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有群主可以解散群");
        }
        group.setStatus(GROUP_STATUS_DISSOLVED);
        imGroupMapper.updateById(group);
    }

    // ==================== 7.3 邀请链接 ====================

    @Override
    @Transactional
    public GroupInviteLinkVo createInviteLink(Long currentUserId, Long groupId, CreateInviteLinkRequestDto requestDto) {
        requireNormalGroup(groupId);
        ImGroupMemberEntity operator = requireActiveMember(groupId, currentUserId);
        if (!canManageMembers(operator)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权创建邀请链接");
        }

        ImGroupInviteEntity invite = new ImGroupInviteEntity();
        invite.setGroupId(groupId);
        invite.setToken(UUID.randomUUID().toString().replace("-", ""));
        invite.setInviterUserId(currentUserId);
        invite.setMaxUses(requestDto.getMaxUses());
        invite.setCurrentUses(0);
        if (requestDto.getExpireHours() != null && requestDto.getExpireHours() > 0) {
            invite.setExpireAt(LocalDateTime.now().plusHours(requestDto.getExpireHours()));
        }
        invite.setStatus(INVITE_STATUS_ACTIVE);
        invite.setCreatedAt(LocalDateTime.now());
        invite.setUpdatedAt(LocalDateTime.now());
        imGroupInviteMapper.insert(invite);

        GroupInviteLinkVo vo = new GroupInviteLinkVo();
        vo.setInviteId(invite.getId());
        vo.setToken(invite.getToken());
        vo.setUrl("/invite/" + invite.getToken());
        vo.setMaxUses(invite.getMaxUses());
        vo.setCurrentUses(0);
        vo.setExpireAt(invite.getExpireAt());
        vo.setCreatedAt(invite.getCreatedAt());
        return vo;
    }

    @Override
    public List<GroupInviteItemVo> listInviteLinks(Long currentUserId, Long groupId) {
        requireNormalGroup(groupId);
        ImGroupMemberEntity operator = requireActiveMember(groupId, currentUserId);
        if (!canManageMembers(operator)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看邀请链接");
        }
        return imGroupInviteMapper.selectActiveInvitesByGroupId(groupId);
    }

    @Override
    @Transactional
    public void revokeInviteLink(Long currentUserId, Long groupId, Long inviteId) {
        requireNormalGroup(groupId);
        ImGroupMemberEntity operator = requireActiveMember(groupId, currentUserId);
        if (!canManageMembers(operator)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权撤销邀请链接");
        }
        ImGroupInviteEntity invite = imGroupInviteMapper.selectById(inviteId);
        if (invite == null || !invite.getGroupId().equals(groupId)) {
            throw new BizException(ErrorCode.INVITE_LINK_NOT_FOUND, "邀请链接不存在");
        }
        invite.setStatus(INVITE_STATUS_REVOKED);
        invite.setUpdatedAt(LocalDateTime.now());
        imGroupInviteMapper.updateById(invite);
    }

    @Override
    public InvitePreviewVo getInvitePreview(String token) {
        ImGroupInviteEntity invite = imGroupInviteMapper.selectValidByToken(token);
        if (invite == null) {
            throw new BizException(ErrorCode.INVITE_LINK_NOT_FOUND, "邀请链接不存在或已失效");
        }
        ImGroupEntity group = imGroupMapper.selectById(invite.getGroupId());
        if (group == null || !Integer.valueOf(GROUP_STATUS_NORMAL).equals(group.getStatus())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "群组不存在");
        }
        ImUserEntity inviter = imUserMapper.selectById(invite.getInviterUserId());

        InvitePreviewVo vo = new InvitePreviewVo();
        vo.setGroupId(group.getId());
        vo.setGroupName(group.getGroupName());
        vo.setAvatarUrl(group.getAvatarUrl());
        vo.setMemberCount(imGroupMemberMapper.countActiveMembers(group.getId()));
        vo.setInviterNickname(inviter != null ? inviter.getNickname() : null);
        return vo;
    }

    @Override
    @Transactional
    public void joinByInvite(Long currentUserId, String token) {
        ImGroupInviteEntity invite = imGroupInviteMapper.selectValidByToken(token);
        if (invite == null) {
            throw new BizException(ErrorCode.INVITE_LINK_NOT_FOUND, "邀请链接不存在或已失效");
        }
        if (invite.getExpireAt() != null && invite.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.INVITE_LINK_EXPIRED, "邀请链接已过期");
        }
        if (invite.getMaxUses() != null && invite.getCurrentUses() >= invite.getMaxUses()) {
            throw new BizException(ErrorCode.INVITE_LINK_EXHAUSTED, "邀请链接已达使用上限");
        }

        Long groupId = invite.getGroupId();
        ImGroupEntity group = requireNormalGroup(groupId);

        ImGroupMemberEntity existing = imGroupMemberMapper.selectByGroupIdAndUserId(groupId, currentUserId);
        if (existing != null && Integer.valueOf(MEMBER_STATUS_NORMAL).equals(existing.getStatus())) {
            throw new BizException(ErrorCode.ALREADY_IN_GROUP, "已经是群成员");
        }

        ensureGroupMember(groupId, currentUserId, MEMBER_ROLE_MEMBER, JOIN_SOURCE_INVITE_LINK);
        ImConversationEntity conversation = ensureCollectiveConversation(group);
        ensureConversationUser(conversation.getId(), currentUserId, false);

        invite.setCurrentUses(invite.getCurrentUses() + 1);
        invite.setUpdatedAt(LocalDateTime.now());
        imGroupInviteMapper.updateById(invite);

        var conversationItem = imConversationMapper.selectConversationItemByUserId(conversation.getId(), currentUserId);
        if (conversationItem != null) {
            imWsPushService.pushConversationChange(currentUserId, "CONVERSATION_CREATED", conversationItem, null);
        }
    }

    // ==================== 7.4 禁言 ====================

    @Override
    @Transactional
    public void muteMember(Long currentUserId, Long groupId, Long userId, MuteMemberRequestDto requestDto) {
        requireNormalGroup(groupId);
        ImGroupMemberEntity operator = requireActiveMember(groupId, currentUserId);
        if (!canManageMembers(operator)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权禁言成员");
        }
        ImGroupMemberEntity target = requireActiveMember(groupId, userId);
        if (Integer.valueOf(MEMBER_ROLE_OWNER).equals(target.getRole())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "不能禁言群主");
        }
        if (Integer.valueOf(MEMBER_ROLE_ADMIN).equals(target.getRole())
                && !Integer.valueOf(MEMBER_ROLE_OWNER).equals(operator.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有群主可以禁言管理员");
        }

        LocalDateTime muteUntil = null;
        if (requestDto.getDurationMinutes() != null && requestDto.getDurationMinutes() > 0) {
            muteUntil = LocalDateTime.now().plusMinutes(requestDto.getDurationMinutes());
        }
        target.setMuteUntil(muteUntil);
        target.setUpdatedAt(LocalDateTime.now());
        imGroupMemberMapper.updateById(target);
    }

    @Override
    @Transactional
    public void unmuteMember(Long currentUserId, Long groupId, Long userId) {
        requireNormalGroup(groupId);
        ImGroupMemberEntity operator = requireActiveMember(groupId, currentUserId);
        if (!canManageMembers(operator)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权解除禁言");
        }
        ImGroupMemberEntity target = requireActiveMember(groupId, userId);
        target.setMuteUntil(null);
        target.setUpdatedAt(LocalDateTime.now());
        imGroupMemberMapper.updateById(target);
    }

    // ==================== 7.4 入群审批 ====================

    @Override
    @Transactional
    public void submitJoinRequest(Long currentUserId, Long groupId, String applyMsg) {
        requireNormalGroup(groupId);

        ImGroupMemberEntity existing = imGroupMemberMapper.selectByGroupIdAndUserId(groupId, currentUserId);
        if (existing != null && Integer.valueOf(MEMBER_STATUS_NORMAL).equals(existing.getStatus())) {
            throw new BizException(ErrorCode.ALREADY_IN_GROUP, "已经是群成员");
        }

        ImGroupJoinRequestEntity pending = imGroupJoinRequestMapper.selectPendingByGroupAndUser(groupId, currentUserId);
        if (pending != null) {
            throw new BizException(ErrorCode.JOIN_REQUEST_PENDING, "已有待审批的申请");
        }

        ImGroupJoinRequestEntity request = new ImGroupJoinRequestEntity();
        request.setGroupId(groupId);
        request.setUserId(currentUserId);
        request.setApplyMsg(applyMsg);
        request.setStatus(JOIN_REQUEST_PENDING);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        imGroupJoinRequestMapper.insert(request);

        // Push notification to admins/owner
        List<ImGroupMemberEntity> admins = imGroupMemberMapper.selectActiveMembersByGroupId(groupId);
        ImUserEntity applicant = imUserMapper.selectById(currentUserId);
        String notifyContent = (applicant != null ? applicant.getNickname() : "用户") + " 申请加入群聊";
        for (ImGroupMemberEntity admin : admins) {
            if (Integer.valueOf(MEMBER_ROLE_OWNER).equals(admin.getRole())
                    || Integer.valueOf(MEMBER_ROLE_ADMIN).equals(admin.getRole())) {
                imWsPushService.pushToUser(admin.getUserId(), WsMessageType.NOTICE, null, null,
                        Map.of("type", "JOIN_REQUEST", "groupId", groupId, "requestId", request.getId()));
            }
        }
    }

    @Override
    @Transactional
    public void reviewJoinRequest(Long currentUserId, Long groupId, Long requestId, ReviewJoinRequestDto requestDto) {
        ImGroupEntity group = requireNormalGroup(groupId);
        ImGroupMemberEntity operator = requireActiveMember(groupId, currentUserId);
        if (!canManageMembers(operator)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权审批入群申请");
        }

        ImGroupJoinRequestEntity joinRequest = imGroupJoinRequestMapper.selectById(requestId);
        if (joinRequest == null || !joinRequest.getGroupId().equals(groupId)
                || !Integer.valueOf(JOIN_REQUEST_PENDING).equals(joinRequest.getStatus())) {
            throw new BizException(ErrorCode.JOIN_REQUEST_NOT_FOUND, "申请不存在或已处理");
        }

        joinRequest.setStatus(requestDto.getApproved() ? JOIN_REQUEST_APPROVED : JOIN_REQUEST_REJECTED);
        joinRequest.setHandledBy(currentUserId);
        joinRequest.setHandledAt(LocalDateTime.now());
        joinRequest.setUpdatedAt(LocalDateTime.now());
        imGroupJoinRequestMapper.updateById(joinRequest);

        if (requestDto.getApproved()) {
            ensureGroupMember(groupId, joinRequest.getUserId(), MEMBER_ROLE_MEMBER, JOIN_SOURCE_JOIN_REQUEST);
            ImConversationEntity conversation = ensureCollectiveConversation(group);
            ensureConversationUser(conversation.getId(), joinRequest.getUserId(), false);
            var conversationItem = imConversationMapper.selectConversationItemByUserId(conversation.getId(), joinRequest.getUserId());
            if (conversationItem != null) {
                imWsPushService.pushConversationChange(joinRequest.getUserId(), "CONVERSATION_CREATED", conversationItem, null);
            }
        }
    }

    @Override
    public List<GroupJoinRequestItemVo> listPendingJoinRequests(Long currentUserId, Long groupId) {
        requireNormalGroup(groupId);
        ImGroupMemberEntity operator = requireActiveMember(groupId, currentUserId);
        if (!canManageMembers(operator)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看入群申请");
        }
        return imGroupJoinRequestMapper.selectPendingItemsByGroupId(groupId);
    }

    private ImGroupEntity requireNormalGroup(Long groupId) {
        ImGroupEntity group = imGroupMapper.selectById(groupId);
        if (group == null || !Integer.valueOf(GROUP_STATUS_NORMAL).equals(group.getStatus())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "群组不存在");
        }
        return group;
    }

    private ImGroupMemberEntity requireActiveMember(Long groupId, Long userId) {
        ImGroupMemberEntity member = imGroupMemberMapper.selectActiveByGroupIdAndUserId(groupId, userId);
        if (member == null) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "群成员不存在");
        }
        return member;
    }

    private boolean canRemove(ImGroupMemberEntity operator, ImGroupMemberEntity target) {
        if (Integer.valueOf(MEMBER_ROLE_OWNER).equals(operator.getRole())) {
            return true;
        }
        return Integer.valueOf(MEMBER_ROLE_ADMIN).equals(operator.getRole())
                && Integer.valueOf(MEMBER_ROLE_MEMBER).equals(target.getRole());
    }

    private boolean canManageMembers(ImGroupMemberEntity operator) {
        return Integer.valueOf(MEMBER_ROLE_OWNER).equals(operator.getRole())
                || Integer.valueOf(MEMBER_ROLE_ADMIN).equals(operator.getRole());
    }

    private boolean canEditMeta(ImGroupMemberEntity operator) {
        return canManageMembers(operator);
    }

    private void ensureGroupMember(Long groupId, Long userId, int role, int joinSource) {
        ImGroupMemberEntity existing = imGroupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (existing != null) {
            existing.setRole(role);
            existing.setJoinSource(joinSource);
            existing.setStatus(MEMBER_STATUS_NORMAL);
            existing.setUpdatedAt(LocalDateTime.now());
            imGroupMemberMapper.updateById(existing);
            return;
        }
        ImGroupMemberEntity member = new ImGroupMemberEntity();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinSource(joinSource);
        member.setJoinAt(LocalDateTime.now());
        member.setStatus(MEMBER_STATUS_NORMAL);
        imGroupMemberMapper.insert(member);
    }

    private ImConversationEntity ensureCollectiveConversation(ImGroupEntity group) {
        ImConversationEntity existing = imConversationMapper.selectCollectiveConversationByGroupId(group.getId());
        if (existing != null) {
            existing.setStatus(CONVERSATION_STATUS_NORMAL);
            existing.setConversationName(group.getGroupName());
            existing.setAvatarUrl(group.getAvatarUrl());
            existing.setConversationType(group.getConversationType());
            existing.setBizKey(buildCollectiveBizKey(group));
            imConversationMapper.updateById(existing);
            return existing;
        }
        ImConversationEntity conversation = new ImConversationEntity();
        conversation.setConversationNo(IdGenerator.conversationNo());
        conversation.setConversationType(group.getConversationType());
        conversation.setBizKey(buildCollectiveBizKey(group));
        conversation.setBizId(group.getId());
        conversation.setConversationName(group.getGroupName());
        conversation.setAvatarUrl(group.getAvatarUrl());
        conversation.setStatus(CONVERSATION_STATUS_NORMAL);
        imConversationMapper.insert(conversation);
        return conversation;
    }

    private void ensureConversationUser(Long conversationId, Long userId, boolean deleted) {
        ImConversationUserEntity existing = imConversationUserMapper.selectByConversationIdAndUserId(conversationId, userId);
        if (existing != null) {
            existing.setDeleted(deleted ? 1 : 0);
            existing.setIsArchived(0);
            existing.setManualUnread(0);
            imConversationUserMapper.updateById(existing);
            return;
        }
        ImConversationUserEntity entity = new ImConversationUserEntity();
        entity.setConversationId(conversationId);
        entity.setUserId(userId);
        entity.setUnreadCount(0);
        entity.setLastReadSeq(0L);
        entity.setIsTop(0);
        entity.setIsMute(0);
        entity.setIsArchived(0);
        entity.setManualUnread(0);
        entity.setDeleted(deleted ? 1 : 0);
        imConversationUserMapper.insert(entity);
    }

    private void pushSystemMessage(Long conversationId, Long groupId, String content) {
        Long seqNo = imMessageMapper.selectMaxSeqNoByConversationId(conversationId);
        long nextSeq = seqNo == null ? 1 : seqNo + 1;
        LocalDateTime now = LocalDateTime.now();

        ImMessageEntity message = new ImMessageEntity();
        message.setConversationId(conversationId);
        message.setConversationType(CONVERSATION_TYPE_GROUP);
        message.setSeqNo(nextSeq);
        message.setClientMsgId("sys-" + UUID.randomUUID());
        message.setFromUserId(0L);
        message.setGroupId(groupId);
        message.setMsgType(6);
        message.setContent(content);
        message.setSendStatus(1);
        message.setSentAt(now);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        imMessageMapper.insert(message);

        imConversationMapper.updateLastMessageState(conversationId, message.getId(), content, now);

        List<ImConversationUserEntity> members = imConversationUserMapper.selectByConversationId(conversationId);
        for (ImConversationUserEntity member : members) {
            if (member.getDeleted() != null && member.getDeleted() == 1) {
                continue;
            }
            var conversationItem = imConversationMapper.selectConversationItemByUserId(conversationId, member.getUserId());
            if (conversationItem != null) {
                imWsPushService.pushConversationChange(member.getUserId(), "MESSAGE_NEW", conversationItem, null);
            }
        }
    }

    private void freezeConversationAtCurrentSeq(Long groupId, Long userId, boolean keepConversation) {
        ImConversationEntity conversation = imConversationMapper.selectCollectiveConversationByGroupId(groupId);
        if (conversation == null) {
            return;
        }
        ImConversationUserEntity conversationUser = imConversationUserMapper.selectByConversationIdAndUserId(conversation.getId(), userId);
        if (conversationUser == null) {
            return;
        }
        Long maxSeqNo = imMessageMapper.selectMaxSeqNoByConversationId(conversation.getId());
        conversationUser.setLastReadSeq(maxSeqNo == null ? 0L : maxSeqNo);
        conversationUser.setUnreadCount(0);
        conversationUser.setDeleted(keepConversation ? 0 : 1);
        imConversationUserMapper.updateById(conversationUser);
    }

    private void pushConversationCreated(Long conversationId, Set<Long> memberIds) {
        for (Long memberId : memberIds) {
            var conversation = imConversationMapper.selectConversationItemByUserId(conversationId, memberId);
            if (conversation != null) {
                imWsPushService.pushConversationChange(memberId, "CONVERSATION_CREATED", conversation, null);
            }
        }
    }

    private Set<Long> normalizeMemberIds(Long currentUserId, List<Long> requestMemberIds) {
        Set<Long> memberIds = new LinkedHashSet<>();
        if (currentUserId != null) {
            memberIds.add(currentUserId);
        }
        if (requestMemberIds != null) {
            memberIds.addAll(requestMemberIds);
        }
        memberIds.remove(null);
        if (memberIds.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "群成员不能为空");
        }
        return memberIds;
    }

    private void validateUsersExist(Set<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return;
        }
        if (imUserMapper.selectBatchIds(memberIds).size() != memberIds.size()) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "群成员不存在");
        }
    }

    private String generateGroupNo() {
        return IdGenerator.groupNo();
    }

    private int normalizeConversationType(Integer conversationType) {
        int resolved = conversationType == null ? CONVERSATION_TYPE_GROUP : conversationType;
        if (resolved != CONVERSATION_TYPE_GROUP && resolved != CONVERSATION_TYPE_CHANNEL) {
            throw new BizException(ErrorCode.PARAM_ERROR, "群组类型错误");
        }
        return resolved;
    }

    private int normalizeEditableRole(Integer role) {
        if (Integer.valueOf(MEMBER_ROLE_MEMBER).equals(role) || Integer.valueOf(MEMBER_ROLE_ADMIN).equals(role)) {
            return role;
        }
        throw new BizException(ErrorCode.PARAM_ERROR, "角色值错误");
    }

    private String buildCollectiveBizKey(ImGroupEntity group) {
        return Integer.valueOf(CONVERSATION_TYPE_CHANNEL).equals(group.getConversationType())
                ? "channel_" + group.getId()
                : "group_" + group.getId();
    }
}
