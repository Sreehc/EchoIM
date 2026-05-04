# EchoIM 二期开发规划

> 创建日期：2026-05-04
> 前置条件：一期工程全部五个阶段已完成，系统具备完整的即时通讯、语音通话、管理后台能力
> 目标：从"功能可用"升级为"体验优秀、安全可靠、可规模运营"
> 最后更新：2026-05-04 — 阶段六全部完成 + 阶段七全部完成 + 阶段八全部完成 + 阶段九全部完成

---

## 一期回顾

一期交付了 EchoIM 的核心骨架：

- 单聊 / 群聊 / 频道实时消息收发
- 语音通话（WebRTC）
- 文件共享（图片、文件、GIF、贴纸）
- 消息撤回、编辑、回复、转发、表情回应
- 好友系统、会话管理、全局搜索、Saved Messages
- 管理后台（用户、群组、靓号、配置、版本）
- 安全机制（JWT + Refresh Token、邮箱验证、可信设备）
- 性能优化（content-visibility、懒加载、消息批量处理）
- 可运维性（链路追踪、健康检查、WS 指标、错误上报）

二期的重点是从"能用"到"好用"，补齐高频 IM 场景中的缺失能力，同时提升安全性和可运营性。

---

## 阶段六：多媒体消息增强

**目标**：补齐消息类型中的体验短板，让媒体消息真正好用。

### 6.1 语音消息

> **审查结论（2026-05-04）**：阶段六 6.1 语音消息全部 4 项任务在代码层面均已实现。
> 前端采用 MediaRecorder API + Web Audio API 实现录音与波形可视化，后端通过 extraJson 存储 voice payload（duration + waveform）。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 6.1.1 | 前端语音录制组件 | P0 | 2d | ✅ 已完成 | `composables/useMediaRecorder.ts`（MediaRecorder + Web Audio API 波形）+ `components/chat/VoiceRecorder.vue`（录音 UI：波形动画、60s 上限、取消/重录/试听） |
| 6.1.2 | 语音消息数据模型 | P0 | 0.5d | ✅ 已完成 | 后端：`ImSingleChatServiceImpl`/`ImGroupChatServiceImpl` MESSAGE_TYPE_VOICE=7 + FILE_BIZ_TYPE_AUDIO=5 + validateVoiceExtra()；前端：`types/chat.ts` VoicePayload + `types/api.ts` voice 字段 + `adapters/chat.ts` adaptVoice() |
| 6.1.3 | 语音消息气泡与播放器 | P0 | 1.5d | ✅ 已完成 | `components/chat/VoicePlayer.vue`（内嵌播放条、波形可视化、播放进度）+ `MessageBubble.vue` VOICE 模板 + `MessageComposer.vue` 麦克风按钮 + `views/chat/ChatHomeView.vue` handleSendVoiceMessage() |
| 6.1.4 | 后端语音消息处理 | P1 | 0.5d | ✅ 已完成 | `vo/message/VoicePayloadVo.java`（voice payload VO）+ `MessageViewServiceImpl` readVoice() 从 extraJson 提取 voice 数据 + `WsMessageItem`/`MessageItemVo` voice 字段 + `FileServiceImpl` BIZ_TYPE_AUDIO=5 + `FileProperties` 音频白名单 |

### 6.2 视频通话

> **审查结论（2026-05-04）**：阶段六 6.2 视频通话全部 4 项任务在代码层面均已实现。
> 后端 CallService 原生支持 audio/video 双类型，前端 CallOverlay 支持视频全屏 + 本地画中画 + 摄像头开关。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 6.2.1 | 视频通话信令扩展 | P0 | 1d | ✅ 已完成 | 后端：`WsMessageType`（CALL_INVITE/ACCEPT/REJECT/CANCEL/END/OFFER/ANSWER/ICE_CANDIDATE/CALL_STATE）、`WsCallSignalData`（callId/conversationId/sdp/candidate）、`ImTextFrameHandler` CALL_OFFER/ANSWER/ICE_CANDIDATE → `callService.relaySignal()`；前端：`services/ws.ts` EchoWsClient（sendCallOffer/sendCallAnswer/sendCallIceCandidate）、`stores/call.ts` handleWsEvent() 处理全部 CALL_* 信令、`types/chat.ts` CallType = 'audio' \| 'video' |
| 6.2.2 | 视频通话 UI | P0 | 2d | ✅ 已完成 | `components/chat/CallOverlay.vue`（全量重写）：视频通话 → 全屏 remote video 背景 + 本地 PiP（140×186px 底部右侧，镜像显示）+ 半透明渐变控制栏；音频通话 → 原有卡片式 UI 不变；`components/chat/ChatTopbar.vue` 下拉菜单增加"发起视频通话"选项；`views/chat/ChatHomeView.vue` handleStartCall(callType) 转发 callType 参数 |
| 6.2.3 | 媒体流管理 | P0 | 1.5d | ✅ 已完成 | `stores/call.ts` ensureLocalMedia()：video=true 时 getUserMedia 带分辨率约束（ideal 1280×720@24fps, max 1920×1080@30fps, facingMode=user）；toggleMute() 控制 audioTrack.enabled；toggleCamera() 控制 videoTrack.enabled；ensurePeerConnection() addTrack 全部轨道 + ontrack 收流；onconnectionstatechange 动态标签（视频/语音） |
| 6.2.4 | 后端 callType 扩展 | P1 | 0.5d | ✅ 已完成 | `service/impl/CallServiceImpl.java`：normalizeCallType() 校验 audio/video；createCall() 设置 callType + 系统消息"发起了视频/语音通话"；pushMissedCallMessage() "未接视频通话/未接来电"；pushDurationMessage() "视频通话时长/通话时长"；buildSummary() callType 字段随 CALL_STATE 推送；`entity/ImCallSessionEntity` callType 字段；`dto/call/CreateCallRequestDto` callType 参数 |

### 6.3 图片消息增强

> **审查结论（2026-05-04）**：阶段六 6.3 图片消息增强全部 3 项任务在代码层面均已实现。
> 后端上传时通过 ImageIO 自动生成 300px 宽缩略图并存储，前端新增全屏图片查看器支持缩放/滑动/下载/转发，消息编辑器支持多图选择批量发送。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 6.3.1 | 图片缩略图生成 | P1 | 1d | ✅ 已完成 | 后端：`ImFileEntity` + `FileInfoVo` thumbnailUrl 字段 + `FileServiceImpl.generateAndStoreThumbnail()`（ImageIO 缩放 300px 宽，JPEG 输出，`_thumb.jpg` 后缀存储）+ `init.sql` thumbnail_url 列；前端：`types/chat.ts` ChatFile.thumbnailUrl + `adapters/chat.ts` adaptFile() 映射 + `MessageBubble.vue` imageDisplayUrl 优先使用缩略图 |
| 6.3.2 | 图片查看器 | P1 | 1d | ✅ 已完成 | `components/chat/ImageViewer.vue`（全屏暗色遮罩 + 鼠标滚轮缩放 0.3x-5x + 指针拖拽平移 + 左右箭头/滑动切换 + 双击复位 + 下载/转发按钮 + 键盘导航 Escape/←/→）+ `MessageBubble.vue` IMAGE 点击 emit open-image-viewer + `MessagePane.vue` 事件透传 + `ChatHomeView.vue` 收集会话全部 IMAGE 消息构建 viewer 列表 |
| 6.3.3 | 多图发送 | P2 | 1d | ✅ 已完成 | `MessageComposer.vue` 文件 input 增加 multiple 属性 + onSelectFile 支持多文件 → emit upload-files + `ChatHomeView.vue` handleUploadMultipleFiles() 逐个上传并发送 IMAGE 消息 + 发送完成后通知计数 |

**验收标准：**
- 单聊中可以录制并发送语音消息，对方能播放
- 单聊中可以发起视频通话，双方能看到对方画面
- 图片消息支持缩略图预览和全屏查看

---

## 阶段七：群组能力深化

**目标**：补齐群聊场景中的高频需求，提升群组管理能力。

### 7.1 群公告与置顶消息

> **审查结论（2026-05-04）**：阶段七 7.1 群公告与置顶消息全部 2 项任务在代码层面均已实现。
> 后端 im_message_pin 表 + PinMessage/UnpinMessage/ListPinned API，前端消息右键菜单置顶/取消置顶 + PinnedMessagesBanner 横幅展示。
> 群公告变更自动推送系统消息到所有成员。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 7.1.1 | 群内消息置顶 | P0 | 1d | ✅ 已完成 | 后端：`ImMessagePinEntity.java` + `ImMessagePinMapper.java` + `MessagePinMapper.xml` + `init.sql` im_message_pin 表 + `MessageCommandServiceImpl` pinMessage/unpinMessage/listPinned + `MessageController` PUT /{id}/pin, PUT /{id}/unpin, GET /pinned + `MessageMapper.xml` LEFT JOIN im_message_pin + `WsMessageType` MESSAGE_PIN/MESSAGE_UNPIN；前端：`services/messages.ts` pinMessage/unpinMessage/listPinnedMessages + `MessageBubble.vue` pin/unpin 右键菜单 + 置顶徽章 + `PinnedMessagesBanner.vue` + `ChatHomeView.vue` handlePinMessage/handleUnpinMessage + `stores/chat.ts` pinMessage/unpinMessage actions + WS 处理 |
| 7.1.2 | 群公告增强 | P1 | 0.5d | ✅ 已完成 | `GroupServiceImpl.java` updateGroup() 检测 notice 变更 → pushSystemMessage() 插入系统消息 + 推送 CONVERSATION_CHANGE 给所有非删除成员 |

### 7.2 @提及

> **审查结论（2026-05-04）**：阶段七 7.2 @提及全部 3 项任务在代码层面均已实现。
> 输入框 @ 触发 MentionSelector 成员选择器，mentions 存入 extraJson，消息气泡中 @ 部分高亮可点击跳转会话，
> 被 @ 用户收到 CONVERSATION_CHANGE 带 atMentionedUserIds 字段，会话列表显示 @ 标记。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 7.2.1 | @提及输入 | P0 | 1.5d | ✅ 已完成 | 前端：`MentionSelector.vue`（成员选择器：过滤/键盘导航/角色徽章）+ `MessageComposer.vue` @ 触发检测 + mention 跟踪 + `ChatHomeView.vue` activeGroupMembers 传递 + `stores/chat.ts` buildMessageExtraWithMentions + sendMessageThroughRealtime mentions 参数；后端：`MentionVo.java` + `MessageItemVo`/`WsMessageItem` mentions 字段 + `MessageViewServiceImpl` readMentions() 从 extraJson 提取 + `types/api.ts` ApiMentionItem + `adapters/chat.ts` adaptChatMessage mentions 映射 |
| 7.2.2 | @提及解析与高亮 | P0 | 1d | ✅ 已完成 | `utils/format.ts` highlightMentions() + highlightBubbleContent()（mention/search 双层高亮）+ `MessageBubble.vue` .message-bubble__mention 高亮样式 + 点击 emit view-profile → openChatFromContact 跳转会话 + `MessagePane.vue` view-profile 事件透传 |
| 7.2.3 | @提及通知 | P1 | 0.5d | ✅ 已完成 | 后端：`ImGroupChatServiceImpl` extractMentionedUserIds() 从 extraJson 解析 + pushConversationChange 传递 atMentionedUserIds + `ImWsPushService` 重载 pushConversationChange 支持 atMentionedUserIds；前端：`types/api.ts` WsConversationChangePayload.atMentionedUserIds + `stores/chat.ts` mentionedConversationIds Set + handleConversationChange 检测 atMentionedUserIds + openConversation 清除 + `ConversationListItem.vue` @ 标记徽章 |

### 7.3 群邀请

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 说明 |
|------|------|--------|---------|------|------|
| 7.3.1 | 邀请链接生成 | P1 | 1d | ✅ 已完成 | `im_group_invite` 表 + `ImGroupInviteEntity` + `ImGroupInviteMapper` + `GroupInviteMapper.xml`(selectValidByToken/selectActiveInvitesByGroupId) + `CreateInviteLinkRequestDto`(maxUses/expireHours) + `GroupInviteLinkVo`/`GroupInviteItemVo` + `GroupServiceImpl.createInviteLink/listInviteLinks/revokeInviteLink` + `GroupController` POST/GET/DELETE `/{groupId}/invites` |
| 7.3.2 | 邀请链接加入 | P1 | 1d | ✅ 已完成 | `InvitePreviewVo`(groupId/groupName/avatarUrl/memberCount/inviterNickname) + `GroupServiceImpl.getInvitePreview/joinByInvite`(校验过期/使用次数/已在群) + `GroupController` GET `/invite/{token}/preview` + POST `/invite/{token}/join`(公开) + `InviteView.vue`(/invite/:token 路由，展示群信息+确认加入) + `router/index.ts` 新增 invite 路由 |
| 7.3.3 | 邀请链接管理 | P2 | 0.5d | ✅ 已完成 | `ConversationProfilePanelBody.vue` 管理区新增"邀请链接"按钮 + `ChatHomeView.vue` inviteDialog 弹窗(列表展示使用次数/过期时间+生成新链接+撤销) + `groups.ts` createInviteLink/fetchInviteLinks/revokeInviteLink API |

### 7.4 群成员权限细化

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 说明 |
|------|------|--------|---------|------|------|
| 7.4.1 | 禁言功能 | P1 | 1d | ✅ 已完成 | `im_group_member` 新增 `mute_until` 字段 + `ImGroupMemberEntity/GroupMemberItemVo` 新增 muteUntil + `GroupMemberMapper.xml` 查询包含 mute_until + `MuteMemberRequestDto`(durationMinutes) + `GroupServiceImpl.muteMember/unmuteMember`(管理员权限校验+不能禁言群主) + `GroupController` PUT/DELETE `/{groupId}/members/{userId}/mute` + `ImGroupChatServiceImpl.validateNotMuted`(发送消息校验) + 前端成员网格显示禁言徽章(剩余时间)+点击禁言/解除禁言 + `ChatHomeView.vue` muteDialog |
| 7.4.2 | 入群审批 | P2 | 1d | ✅ 已完成 | `im_group_join_request` 表 + `ImGroupJoinRequestEntity` + `ImGroupJoinRequestMapper` + `GroupJoinRequestMapper.xml` + `ReviewJoinRequestDto`(approved) + `GroupJoinRequestItemVo` + `GroupServiceImpl.submitJoinRequest/reviewJoinRequest/listPendingJoinRequests`(WS 推送通知管理员) + `GroupController` POST/GET/PUT `/{groupId}/join-requests` + `ConversationProfilePanelBody.vue` 新增"入群审批"按钮 + `ChatHomeView.vue` joinRequestDialog(同意/拒绝) |

**验收标准：**
- 群聊中可以置顶重要消息，所有成员可见横幅
- 输入 @ 弹出成员选择器，被 @ 的用户收到特殊提醒
- 群主可以生成邀请链接，新用户通过链接加入群聊
- 管理员可禁言/解除禁言成员，被禁言者发送消息时收到提示
- 非邀请链接入群需管理员审批，审批结果实时推送

---

## 阶段八：安全与隐私

**目标**：提升系统安全等级，保护用户隐私。

### 8.1 用户屏蔽

> **审查结论（2026-05-04）**：阶段八 8.1 用户屏蔽全部 3 项任务在代码层面均已实现。
> 后端 im_block_user 表 + BlockService + BlockController 提供完整的屏蔽/取消屏蔽/列表 API，
> 单聊发送消息时双向校验屏蔽关系，群聊消息推送过滤已屏蔽用户，在线状态通知也过滤屏蔽关系。
> 前端用户资料页新增"屏蔽用户/取消屏蔽"按钮，设置页新增"隐私"分组展示已屏蔽用户列表。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 8.1.1 | 屏蔽用户 | P0 | 1d | ✅ 已完成 | 后端：`init.sql` im_block_user 表 + `ImBlockUserEntity.java` + `ImBlockUserMapper.java` + `BlockUserMapper.xml` + `BlockedUserItemVo.java` + `BlockService.java` + `BlockServiceImpl.java`（自屏蔽/目标存在/重复屏蔽校验）+ `BlockController.java`（POST/DELETE/GET /api/blocks）+ `ErrorCode.java` USER_BLOCKED/ALREADY_BLOCKED/NOT_BLOCKED；前端：`services/blocks.ts`（blockUser/unblockUser/fetchBlockedUsers）+ `types/api.ts` ApiBlockedUserItem + `PublicProfileView.vue` 屏蔽/取消屏蔽按钮 + 危险操作样式 |
| 8.1.2 | 屏蔽生效 | P0 | 1d | ✅ 已完成 | 后端：`ImSingleChatServiceImpl.validateNotBlocked()` 双向校验 + `ImGroupChatServiceImpl.sendGroup()` recipientUserIds 过滤已屏蔽用户 + `ImOnlineService.notifyPresenceToPeers()` 过滤已屏蔽用户 + `UserMapper.xml` selectPublicProfileByUserId/selectPublicProfileByUsername 新增 blocked 字段（EXISTS 子查询 im_block_user）+ `UserPublicProfileVo.java` blocked 字段；前端：`types/api.ts` ApiUserPublicProfile.blocked + `PublicProfileView.vue` 发消息按钮 disabled 当 blocked=true |
| 8.1.3 | 屏蔽列表管理 | P1 | 0.5d | ✅ 已完成 | 前端：`types/chat.ts` SettingsSection 新增 'privacy' + `stores/auth.ts` blockedUsers/blockedUsersLoading/loadBlockedUsers/handleUnblockUser + `ConversationSidebar.vue` settingsSections 新增隐私分组 + 已屏蔽用户列表（头像/昵称/用户号/取消屏蔽按钮）+ `ChatHomeView.vue` blockedUsers/blockedUsersLoading props + refresh-blocked-users/unblock-user 事件处理 |

### 8.2 消息安全

> **审查结论（2026-05-04）**：阶段八 8.2 消息安全全部 3 项任务在代码层面均已实现。
> 阅后即焚：前端 MessageComposer 自毁时间选择器（5s/30s/1m/5m/1h），selfDestructSeconds 存入 extraJson，
> MessageBubble 读取后启动倒计时，到期触发 recallMessage 销毁消息。
> 双向删除：后端 deleteForEveryone API 实际删除消息记录并推送 MESSAGE_DELETE WS 事件，
> 前端右键菜单"删除并撤回"选项，双方消息列表同步移除。
> 敏感内容过滤：后端 im_sensitive_word 表 + SensitiveWordService 缓存式词库，
> 单聊/群聊发送时自动过滤，拦截类词汇直接拒绝，标记类词汇替换为星号。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 8.2.1 | 阅后即焚 | P1 | 1.5d | ✅ 已完成 | 前端：`types/chat.ts` ChatMessage selfDestructSeconds/selfDestructAt + `MessageComposer.vue` SELF_DESTRUCT_OPTIONS 自毁时间选择器 + emit send 增加 selfDestructSeconds 参数 + `MessageBubble.vue` selfDestructCountdown 倒计时逻辑 + 自毁徽章 Timer 图标 + `MessagePane.vue` self-destruct-message 事件透传 + `ChatHomeView.vue` handleSendTextMessage 传递 selfDestructSeconds + handleSelfDestructMessage 触发 recallMessage + `stores/chat.ts` sendMessage payload selfDestructSeconds + buildMessageExtra 包含 selfDestructSeconds；后端：`MessageItemVo.java`/`WsMessageItem.java` selfDestructSeconds Integer 字段 + `MessageViewServiceImpl` readInteger() + applyExtra() 读取 selfDestructSeconds |
| 8.2.2 | 消息删除（双向） | P1 | 0.5d | ✅ 已完成 | 后端：`MessageCommandService` 新增 deleteForEveryone 接口 + `MessageCommandServiceImpl` deleteForEveryone()（校验 → 通知删除 → 删除 pin/reaction/message → 刷新会话预览）+ `MessageController` DELETE /api/messages/{id} + `WsMessageType` MESSAGE_DELETE + `ImMessageMapper` selectLatestByConversationId + `MessageMapper.xml` 对应 SQL；前端：`services/messages.ts` deleteMessage() + `stores/chat.ts` deleteMessage action + MESSAGE_DELETE WS 事件处理 + `MessageBubble.vue` delete-for-everyone emit + 'delete' context command + "删除并撤回"菜单项 + `MessagePane.vue` delete-message 事件透传 + `ChatHomeView.vue` handleDeleteMessage 确认弹窗 + 调用 chatStore.deleteMessage |
| 8.2.3 | 敏感内容过滤 | P2 | 1d | ✅ 已完成 | 后端：`init.sql` im_sensitive_word 表 + `ImSensitiveWordEntity.java` + `ImSensitiveWordMapper.java` + `SensitiveWordMapper.xml` + `SensitiveWordService` 接口 + `SensitiveWordServiceImpl`（缓存式词库、containsBlockedWords/filterContent/addSensitiveWord/removeSensitiveWord/reloadCache）+ `ImSingleChatServiceImpl`/`ImGroupChatServiceImpl` 发送时敏感词过滤（拦截/标记）+ `AdminSensitiveWordController` GET/POST/DELETE /api/admin/sensitive-words + POST /reload |

### 8.3 登录安全增强

> **审查结论（2026-05-04）**：阶段八 8.3 登录安全增强全部 3 项任务在代码层面均已实现。
> 8.3.1 登录设备管理：已在一期实现，后端 im_trusted_device 表 + TrustedDeviceService + AuthController，
> 前端 ConversationSidebar 安全分组展示受信设备列表，支持单个/全部移除。
> 8.3.2 登录异常检测：已在一期实现，后端 im_security_event 表 + SecurityEventService，
> 前端安全记录时间线展示，异常登录自动记录。
> 8.3.3 两步验证：后端 TOTP（dev.samstevens.totp 库）+ Redis 挑战状态 + 恢复码机制，
> 登录流程集成 TOTP 验证，前端设置页 TOTP 管理 UI（启用/禁用/QR 码/恢复码）。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 8.3.1 | 登录设备管理 | P1 | 1d | ✅ 已完成（一期） | 后端：`im_trusted_device` 表 + `TrustedDeviceService` + `AuthController`（GET/DELETE /trusted-devices）；前端：`ConversationSidebar.vue` 安全分组"受信设备"列表 + `stores/auth.ts` loadTrustedDevices/revokeTrustedDevice/revokeAllTrustedDevices |
| 8.3.2 | 登录异常检测 | P2 | 1d | ✅ 已完成（一期） | 后端：`im_security_event` 表 + `SecurityEventService` + `AuthController`（GET /security-events）；前端：`ConversationSidebar.vue` 安全分组"安全记录"时间线 + `stores/auth.ts` loadSecurityEvents |
| 8.3.3 | 两步验证 | P2 | 2d | ✅ 已完成 | 后端：`dev.samstevens.totp:totp:2.7.1` 依赖 + `TotpService`/`TotpServiceImpl`（generateSecret/generateUri/verifyCode/generateRecoveryCodes/verifyRecoveryCode/consumeRecoveryCode）+ `ImUserEntity` totpSecret/totpEnabled/recoveryCodes 字段 + `AuthServiceImpl` login() TOTP 挑战流程 + `AuthController` GET /totp/status, POST /totp/setup, POST /totp/enable, POST /totp/disable, POST /login/totp/verify + Redis TOTP_CHALLENGE/TOTP_SETUP 状态管理（5min/10min 过期）+ `TotpSetupVo`/`TotpStatusVo`；前端：`services/auth.ts` fetchTotpStatus/setupTotp/enableTotp/disableTotp/verifyTotpLogin + `stores/auth.ts` totpEnabled/totpRecoveryCodesRemaining/totpLoading + loadTotpStatus/setupTotp/enableTotp/disableTotp/verifyTotpLogin + `LoginView.vue` totp 视图模式 + TOTP 验证表单 + `ConversationSidebar.vue` 两步验证管理 UI（QR 码/恢复码/启用/禁用）+ `ChatHomeView.vue` TOTP 事件处理 |

**验收标准：**
- 屏蔽用户后不再收到对方消息
- 阅后即焚消息在指定时间后自动销毁
- 用户可以查看并管理已登录设备
- 用户可以启用两步验证（TOTP），登录时需要验证器应用验证码

---

## 阶段九：消息能力补全

**目标**：补齐高频 IM 场景中缺失的消息能力。

### 9.1 消息定时发送

> **实现完成（2026-05-04）**：阶段九 9.1 消息定时发送全部 3 项任务在代码层面均已实现。
> 后端 im_scheduled_message 表 + ScheduledMessageService 定时任务执行（@Scheduled fixedDelay=5000），
> 前端 MessageComposer 添加定时发送按钮（Clock 图标）和日期/时间选择器，
> ScheduledMessagesPanel 组件展示待发送消息列表，支持取消和立即发送。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 9.1.1 | 定时发送 UI | P1 | 1d | ✅ 已完成 | 前端：`MessageComposer.vue` Clock 图标导入 + scheduledSendOpen/scheduledDate/scheduledTime refs + minScheduledDateTime/canSchedule 计算属性 + submitScheduled() 函数 + 定时发送按钮和日期/时间选择器 UI + `services/scheduledMessages.ts` createScheduledMessage API + `types/chat.ts` ScheduledMessage 接口和 SCHEDULED_MESSAGE_STATUS 常量 |
| 9.1.2 | 定时任务执行 | P1 | 1d | ✅ 已完成 | 后端：`init.sql` im_scheduled_message 表 + `ImScheduledMessageEntity.java`（状态常量：STATUS_PENDING=1/STATUS_SENT=2/STATUS_CANCELLED=3/STATUS_FAILED=4）+ `ImScheduledMessageMapper.java` + `ScheduledMessageMapper.xml` + `ScheduledMessageService.java` 接口 + `ScheduledMessageServiceImpl.java`（@Scheduled(fixedDelay=5000) 定时执行 + executePendingMessages() + executeScheduledMessage() 创建 WsMessage 调用 singleChatService/groupChatService）+ `ScheduledMessageController.java` REST API + `EchoImServerApplication.java` @EnableScheduling |
| 9.1.3 | 定时消息管理 | P2 | 0.5d | ✅ 已完成 | 前端：`ScheduledMessagesPanel.vue`（Props: conversationId/visible + Emits: close/message-sent + 加载/取消/立即发送功能 + formatScheduledTime/formatMessageType 辅助函数）+ `ChatHomeView.vue` scheduledPanelOpen ref + handleScheduledSend/handleScheduledMessageSent 函数 + MessageComposer @open-scheduled-panel 事件处理 + ScheduledMessagesPanel 组件集成 + `MessageComposer.vue` 日历图标按钮（emit open-scheduled-panel） |

### 9.2 消息草稿

> **实现完成（2026-05-04）**：阶段九 9.2 消息草稿全部 2 项任务在代码层面均已实现。
> 前端 MessageComposer 实现草稿自动保存到 localStorage（500ms 防抖），
> 同步到后端 im_conversation_user.draft_content 字段（1000ms 防抖），
> ConversationListItem 显示"草稿"标记，支持多端一致性。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 9.2.1 | 草稿自动保存 | P1 | 0.5d | ✅ 已完成 | 前端：`MessageComposer.vue` conversationId prop + DRAFT_STORAGE_KEY + getDraftKey() + loadDraftFromStorage() + saveDraftToStorage() + watch conversationId 加载草稿 + watch draft 500ms 防抖保存 + submit/submitScheduled 清除草稿 + `ChatHomeView.vue` conversation-id prop 传递 |
| 9.2.2 | 草稿同步 | P2 | 1d | ✅ 已完成 | 后端：`init.sql` im_conversation_user 新增 draft_content 字段 + `ImConversationUserEntity.java` draftContent 字段 + `ConversationService.java` saveDraft/loadDraft 接口 + `ConversationServiceImpl.java` 实现 + `ConversationController.java` PUT/GET /{id}/draft API + `ConversationMapper.xml` ConversationItemColumns 新增 cu.draft_content AS draftContent + `ConversationItemVo.java` draftContent 字段；前端：`services/drafts.ts` saveDraft/loadDraft API + `MessageComposer.vue` syncDraftToBackend() 1000ms 防抖同步 + `types/chat.ts` ConversationSummary.draftContent + `ConversationListItem.vue` hasDraft 计算属性（优先检查后端 draftContent）+ "草稿"标记样式 |

### 9.3 消息已读详情

> **实现完成（2026-05-04）**：阶段九 9.3 消息已读详情全部 2 项任务在代码层面均已实现。
> 后端 im_message_receipt 表已有完整数据支撑，新增 GET /api/messages/{id}/receipts 接口查询群消息已读详情，
> 返回已读/未读成员列表及最后阅读时间。前端 MessageReadDetails 组件展示已读/未读 Tab 列表，
> MessageBubble 右键菜单新增"已读详情"选项。单聊消息气泡鼠标悬停状态标识可查看送达/已读时间。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 9.3.1 | 群消息已读详情 | P1 | 1d | ✅ 已完成 | 后端：`MessageReadDetailVo.java` + `MessageReadDetailItemVo.java` + `ImMessageReceiptMapper.selectReadDetailsByMessageId` + `MessageReceiptMapper.xml` 新增 SQL + `MessageViewServiceImpl.getMessageReadDetails()`（校验群成员权限）+ `MessageController` GET `/{id}/receipts` + `ErrorCode.MESSAGE_NOT_FOUND`；前端：`MessageReadDetails.vue`（el-dialog + el-tabs 已读/未读列表 + AvatarBadge）+ `services/messages.ts` getMessageReadDetails() + `types/chat.ts` MessageReadDetail/MessageReadDetailItem + `MessageBubble.vue` 'read-details' 右键菜单项 + `MessagePane.vue` view-read-details 事件透传 + `ChatHomeView.vue` handleViewReadDetails + MessageReadDetails 组件集成 |
| 9.3.2 | 消息送达/已读时间 | P2 | 0.5d | ✅ 已完成 | 前端：`MessageBubble.vue` statusTooltip 计算属性（拼接送达时间/已读时间）+ 状态标识 title 属性展示 tooltip + formatReadTime 辅助函数；后端数据已由 MessageViewServiceImpl.enrichMessages() 填充 deliveredAt/readAt 字段 |

### 9.4 消息搜索增强

> **实现完成（2026-05-04）**：阶段九 9.4 消息搜索增强全部 3 项任务在代码层面均已实现。
> 全局搜索结果中消息预览关键词高亮（highlightText），搜索对话框新增消息类型筛选下拉框和日期范围选择器，
> 后端 GlobalSearchService/ImMessageMapper 支持 msgType/dateFrom/dateTo 过滤条件。

| 序号 | 任务 | 优先级 | 预估工时 | 状态 | 实现文件 |
|------|------|--------|---------|------|---------|
| 9.4.1 | 搜索结果高亮 | P1 | 0.5d | ✅ 已完成 | 前端：`ChatHomeView.vue` 消息搜索结果预览使用 highlightText() 高亮关键词 + `.search-sheet__highlight` 样式 + `utils/format.ts` highlightText 函数已存在 |
| 9.4.2 | 按类型筛选 | P2 | 0.5d | ✅ 已完成 | 后端：`GlobalSearchService` 新增 search 重载（msgType/dateFrom/dateTo 参数）+ `GlobalSearchServiceImpl.normalizeMsgType()` 类型映射 + `ImMessageMapper.selectGlobalSearchMessages` 新增 msgType 参数 + `MessageMapper.xml` 动态 AND 条件 + `SearchController` 新增 msgType 请求参数；前端：`services/search.ts` searchGlobal 新增 msgType 参数 + `ChatHomeView.vue` globalSearchState.filterType + el-select 消息类型下拉框 |
| 9.4.3 | 日期范围筛选 | P2 | 0.5d | ✅ 已完成 | 后端：同 9.4.2，`MessageMapper.xml` 新增 dateFrom/dateTo 动态条件 + `SearchController` @DateTimeFormat 参数；前端：`ChatHomeView.vue` globalSearchState.filterDateFrom/filterDateTo + el-date-picker 日期选择器 + 日期转 ISO 传参 |

**验收标准：**
- 消息可以设置定时发送，到期自动投递 ✅
- 群聊中可以查看每条消息的已读详情 ✅
- 全局搜索支持关键词高亮和类型筛选 ✅

---

## 阶段十：管理后台增强

**目标**：提升运营管理效率，增强平台治理能力。

### 10.1 数据看板

| 序号 | 任务 | 优先级 | 预估工时 | 说明 |
|------|------|--------|---------|------|
| 10.1.1 | 核心指标概览 | P0 | 2d | 总用户数、日活/月活、消息量趋势、新增用户趋势、在线峰值，ECharts 图表 |
| 10.1.2 | 实时在线统计 | P1 | 1d | 当前在线用户数、各时段分布、WebSocket 连接数 |
| 10.1.3 | 消息统计 | P2 | 1d | 各类型消息占比、群聊/单聊消息比例、高峰时段分析 |

### 10.2 内容审核

| 序号 | 任务 | 优先级 | 预估工时 | 说明 |
|------|------|--------|---------|------|
| 10.2.1 | 举报机制 | P0 | 1d | 用户可举报消息/用户，举报原因分类，举报记录存入 im_report 表 |
| 10.2.2 | 举报处理 | P1 | 1d | 管理后台举报列表，审核处理（忽略/警告/禁言/封号），处理结果通知举报人 |
| 10.2.3 | 敏感词管理 | P1 | 0.5d | 管理后台敏感词库 CRUD，支持正则，实时生效 |

### 10.3 运营工具

| 序号 | 任务 | 优先级 | 预估工时 | 说明 |
|------|------|--------|---------|------|
| 10.3.1 | 系统公告 | P1 | 1d | 管理后台发布系统公告（全员/指定用户），用户端弹窗或消息形式展示 |
| 10.3.2 | 用户封禁增强 | P1 | 0.5d | 支持临时封禁（指定时长）、封禁原因、封禁历史记录 |
| 10.3.3 | 操作日志 | P2 | 1d | 管理后台操作日志记录（登录、用户操作、群组操作），可查询可导出 |

**验收标准：**
- 管理后台首页展示核心运营数据看板
- 用户可以举报违规内容，管理员可以在后台审核处理
- 管理员可以发布系统公告推送给用户

---

## 阶段十一：多端与工程化

**目标**：提升工程质量和多端体验。

### 11.1 PWA 支持

| 序号 | 任务 | 优先级 | 预估工时 | 说明 |
|------|------|--------|---------|------|
| 11.1.1 | Service Worker | P1 | 1d | 离线缓存静态资源，消息队列离线暂存，上线后自动发送 |
| 11.1.2 | Web Push 通知 | P0 | 1.5d | Push API + Notification API，离线时接收消息推送，需用户授权 |
| 11.1.3 | 安装到桌面 | P2 | 0.5d | manifest.json 配置，安装提示，自定义启动画面 |

### 11.2 国际化

| 序号 | 任务 | 优先级 | 预估工时 | 说明 |
|------|------|--------|---------|------|
| 11.2.1 | i18n 框架接入 | P2 | 1d | vue-i18n 接入，提取所有中文文案到语言包 |
| 11.2.2 | 英文语言包 | P2 | 2d | 补充英文翻译，语言切换 UI |

### 11.3 工程化提升

| 序号 | 任务 | 优先级 | 预估工时 | 说明 |
|------|------|--------|---------|------|
| 11.3.1 | E2E 测试覆盖 | P1 | 3d | Playwright 测试：登录流程、单聊收发、群聊收发、消息撤回编辑、通话流程 |
| 11.3.2 | CI/CD 流水线 | P1 | 2d | GitHub Actions：lint + typecheck + test + build + deploy，自动部署到测试环境 |
| 11.3.3 | 后端单元测试 | P2 | 3d | 核心 Service 层单元测试覆盖（AuthService、ConversationService、MessageService） |
| 11.3.4 | API 版本管理 | P2 | 1d | /api/v1/ 前缀，为后续 API 演进做准备 |

**验收标准：**
- 浏览器关闭后重新打开能收到离线消息推送
- 系统支持中英文切换
- CI 流水线自动运行测试并部署

---

## 里程碑时间线

```
阶段六 ──────────── 2.5 周 ──────────────────┐
  多媒体消息增强                                  │
                                               ├─→ Alpha 2
阶段七 ──────────── 2.5 周 ──────────────────┤
  群组能力深化                                    │
                                               │
阶段八 ──────────── 2 周 ────────────────────┤
  安全与隐私                                      │
                                               ├─→ Beta 2
阶段九 ──────────── 2 周 ────────────────────┤
  消息能力补全                                    │
                                               │
阶段十 ──────────── 2 周 ────────────────────┤
  管理后台增强                                    │
                                               │
阶段十一 ─────────── 3 周 ────────────────────┘
  多端与工程化
```

| 里程碑 | 时间点 | 交付物 |
|--------|--------|--------|
| M6 - 多媒体可用 | 第 2.5 周末 | 语音消息、视频通话、图片增强 |
| M7 - 群组增强 | 第 5 周末 | 置顶消息、@提及、邀请链接、禁言 |
| M8 - 安全加固 | 第 7 周末 | 用户屏蔽、阅后即焚、设备管理 |
| M9 - 消息补全 | 第 9 周末 | 定时发送、草稿同步、已读详情、搜索增强 |
| M10 - 运营增强 | 第 11 周末 | 数据看板、举报审核、系统公告 |
| M11 - 生产级就绪 | 第 14 周末 | PWA、i18n、E2E 测试、CI/CD |

---

## 优先级总览

### P0 — 必须做（用户核心体验）

| 功能 | 阶段 | 说明 |
|------|------|------|
| 语音消息 | 六 | 文字/图片之后最高频的消息类型 |
| 视频通话 | 六 | 语音通话的自然延伸 |
| 群消息置顶 | 七 | 群管理核心需求 |
| @提及 | 七 | 群聊中最常用的交互方式 |
| 用户屏蔽 | 八 | 基础隐私保护 |
| Web Push 通知 | 十一 | 离线消息触达的关键 |
| 数据看板 | 十 | 运营决策基础 |
| 举报机制 | 十 | 内容治理入口 |

### P1 — 应该做（体验提升）

| 功能 | 阶段 | 说明 |
|------|------|------|
| 图片查看器 | 六 | 媒体消息体验闭环 |
| 邀请链接 | 七 | 群增长入口 |
| 禁言功能 | 七 | 群管理必备 |
| 阅后即焚 | 八 | 隐私场景需求 |
| 登录设备管理 | 八 | 账号安全 |
| 定时发送 | 九 | 效率工具 |
| 群消息已读详情 | 九 | 群管理参考 |
| 举报处理 | 十 | 内容审核闭环 |
| 系统公告 | 十 | 运营触达 |
| E2E 测试 | 十一 | 质量保障 |
| CI/CD | 十一 | 开发效率 |

### P2 — 可以做（锦上添花）

| 功能 | 阶段 | 说明 |
|------|------|------|
| 多图发送 | 六 | 体验优化 |
| 入群审批 | 七 | 高级群管理 |
| 敏感内容过滤 | 八 | 合规需求 |
| 两步验证 | 八 | 高安全场景 |
| 消息搜索增强 | 九 | 效率提升 |
| 操作日志 | 十 | 审计需求 |
| 国际化 | 十一 | 出海准备 |
| 后端单元测试 | 十一 | 长期质量 |

---

## 技术风险与注意事项

### 高风险项

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 视频通话 NAT 穿透 | 视频通话质量 | 部署 TURN 服务器（coturn），提供可靠中继 |
| 语音消息文件大小 | 存储和带宽 | Opus 编码压缩，限制最大时长，CDN 加速 |
| Web Push 兼容性 | 通知到达率 | Safari/Chrome/Firefox 分别适配，降级为应用内通知 |
| 阅后即焚防截屏 | 安全性 | Web 端截屏检测有限，明确为"尽力而为"，不做 100% 保证 |
| 大群 @提及性能 | 消息延迟 | @ 信息存 extraJson，推送时过滤，不触发全员广播 |

### 新增依赖

| 依赖 | 用途 | 准备工作 |
|------|------|---------|
| TURN 服务器 | 视频通话中继 | 部署 coturn 或接入第三方（如 Twilio） |
| Push 服务 | Web 推送通知 | 配置 VAPID 密钥，接入 FCM（Chrome）/ Web Push（Firefox） |
| ECharts | 数据看板图表 | 前端 npm 依赖 |
| vue-i18n | 国际化 | 前端 npm 依赖 |
| Playwright | E2E 测试 | CI 环境安装 |

---

## 数据库变更预估

### 新增表

| 表名 | 用途 | 阶段 |
|------|------|------|
| im_message_pin | 群消息置顶 | 七 |
| im_block_user | 用户屏蔽关系 | 八 |
| im_report | 举报记录 | 十 |
| im_system_notice | 系统公告 | 十 |
| im_admin_operation_log | 管理操作日志 | 十 |
| im_login_device | 登录设备记录 | 八 |
| im_scheduled_message | 定时消息 | 九 |

### 变更表

| 表名 | 变更 | 阶段 |
|------|------|------|
| im_message | 新增 msgType=VOICE, extraJson 扩展 | 六 |
| im_file | 新增 thumbnailUrl, waveform 字段 | 六 |
| im_group_member | 新增 muteUntil 禁言截止时间 | 七 |
| im_conversation_user | 新增 draft_content 草稿字段 | 九 |
| im_message_read | 新增 readAt 精确已读时间 | 九 |
