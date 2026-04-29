package com.echoim.server.service.impl;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.dto.group.AddGroupMembersRequestDto;
import com.echoim.server.dto.group.CreateGroupRequestDto;
import com.echoim.server.dto.group.UpdateGroupMemberRoleRequestDto;
import com.echoim.server.dto.group.UpdateGroupRequestDto;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.entity.ImGroupEntity;
import com.echoim.server.entity.ImGroupMemberEntity;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImGroupMapper;
import com.echoim.server.mapper.ImGroupMemberMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.mapper.ImUserMapper;
import com.echoim.server.service.group.GroupService;
import com.echoim.server.vo.group.GroupCreateVo;
import com.echoim.server.vo.group.GroupDetailVo;
import com.echoim.server.vo.group.GroupMemberItemVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
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

    private final ImGroupMapper imGroupMapper;
    private final ImGroupMemberMapper imGroupMemberMapper;
    private final ImConversationMapper imConversationMapper;
    private final ImConversationUserMapper imConversationUserMapper;
    private final ImMessageMapper imMessageMapper;
    private final ImUserMapper imUserMapper;
    private final ImWsPushService imWsPushService;

    public GroupServiceImpl(ImGroupMapper imGroupMapper,
                            ImGroupMemberMapper imGroupMemberMapper,
                            ImConversationMapper imConversationMapper,
                            ImConversationUserMapper imConversationUserMapper,
                            ImMessageMapper imMessageMapper,
                            ImUserMapper imUserMapper,
                            ImWsPushService imWsPushService) {
        this.imGroupMapper = imGroupMapper;
        this.imGroupMemberMapper = imGroupMemberMapper;
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imMessageMapper = imMessageMapper;
        this.imUserMapper = imUserMapper;
        this.imWsPushService = imWsPushService;
    }

    @Override
    @Transactional
    public GroupCreateVo createGroup(Long currentUserId, CreateGroupRequestDto requestDto) {
        Set<Long> memberIds = normalizeMemberIds(currentUserId, requestDto.getMemberIds());
        validateUsersExist(memberIds);
        int conversationType = normalizeConversationType(requestDto.getConversationType());

        ImGroupEntity group = new ImGroupEntity();
        group.setGroupNo(generateGroupNo());
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
        if (StringUtils.hasText(requestDto.getGroupName())) {
            group.setGroupName(requestDto.getGroupName().trim());
            touched = true;
        }
        if (requestDto.getNotice() != null) {
            group.setNotice(requestDto.getNotice().trim());
            touched = true;
        }
        if (!touched) {
            throw new BizException(ErrorCode.PARAM_ERROR, "没有可更新的内容");
        }

        imGroupMapper.updateById(group);
        ensureCollectiveConversation(group);
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
        return "G" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
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
