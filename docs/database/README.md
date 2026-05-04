# EchoIM 数据库设计

## 1. 概述

- 数据库：MySQL 8.4
- 字符集：utf8mb4 / utf8mb4_general_ci
- 存储引擎：InnoDB
- 共 20 张业务表 + 4 张系统表

## 2. 表结构总览

### 2.1 用户与认证

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `im_user` | 用户账号 | id, user_no, username, password_hash, nickname, avatar_url, gender, phone, email, signature, status, totp_secret, totp_enabled, recovery_codes |
| `im_auth_trusted_device` | 可信设备 | user_id, device_name, device_fingerprint, grant_token_hash, expire_at |
| `im_auth_security_event` | 安全事件日志 | user_id, event_type, event_status, ip, user_agent, detail_json |

### 2.2 社交关系

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `im_friend` | 好友关系 | user_id, friend_user_id, remark, status |
| `im_friend_request` | 好友申请 | from_user_id, to_user_id, apply_msg, status |
| `im_block_user` | 用户屏蔽 | user_id, blocked_user_id |

### 2.3 会话与消息

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `im_conversation` | 会话 | conversation_no(UUID), conversation_type(1单聊/2群聊/3频道), biz_key, biz_id, last_message_id, last_message_preview |
| `im_conversation_user` | 会话成员 | conversation_id, user_id, unread_count, last_read_seq, is_top, is_mute, is_archived, draft_content |
| `im_message` | 消息 | conversation_id, seq_no(会话内递增), from_user_id, msg_type(1文本/2贴纸/3图片/4GIF/5文件/6系统/7语音), content, extra_json(JSON扩展), file_id, send_status |
| `im_message_receipt` | 消息回执 | message_id, user_id, receipt_type(1送达/2已读), receipt_at |
| `im_message_reaction` | 表情回应 | message_id, user_id, emoji |
| `im_message_pin` | 消息置顶 | conversation_id, message_id, pinned_by_user_id |

### 2.4 群组

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `im_group` | 群组/频道 | group_no, group_name, owner_user_id, conversation_type, notice, status |
| `im_group_member` | 群成员 | group_id, user_id, role(1群主/2成员/3管理员), nick_name, mute_until |
| `im_group_invite` | 邀请链接 | group_id, token, inviter_user_id, max_uses, expire_at |
| `im_group_join_request` | 入群申请 | group_id, user_id, apply_msg, status, handled_by |

### 2.5 文件与通话

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `im_file` | 文件资源 | owner_user_id, biz_type(1头像/2图片/3视频/4文件/5音频), storage_type, object_key, file_name, content_type, file_size, url, thumbnail_url |
| `im_call_session` | 通话记录 | conversation_id, call_type(audio/video), caller_user_id, callee_user_id, status, started_at, answered_at, ended_at |

### 2.6 定时消息与草稿

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `im_scheduled_message` | 定时消息 | user_id, conversation_id, msg_type, content, scheduled_at, status(1待发/2已发/3已取消/4失败), sent_message_id |

草稿存储在 `im_conversation_user.draft_content` 字段。

### 2.7 安全与内容审核

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `im_sensitive_word` | 敏感词库 | word, category, level(1普通/2严重), action(1标记/2拦截) |
| `im_report` | 举报记录 | reporter_user_id, target_type(1消息/2用户), target_id, reason, status(0待处理/1忽略/2警告/3禁言/4封号) |
| `im_user_ban` | 用户封禁 | user_id, ban_type(1临时/2永久), reason, ban_minutes, expire_at, status |

### 2.8 运营管理

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `im_system_notice` | 系统公告 | title, content, notice_type(1全员/2指定用户), target_user_ids(JSON), status(1发布/2撤回) |
| `sys_admin_user` | 管理员 | username, password_hash, nickname, role_code |
| `sys_config` | 系统配置 | config_key, config_value, config_name |
| `sys_version` | 版本信息 | version_code, version_name, platform, force_update, gray_percent |
| `sys_beauty_no` | 靓号池 | beauty_no, bind_user_id, level_type |
| `sys_operation_log` | 操作日志 | admin_user_id, module_name, action_name, target_type, target_id, content_json |

## 3. 核心表详细设计

### 3.1 im_message

消息表是系统核心，使用 `seq_no` 实现会话内消息有序递增。

```sql
CREATE TABLE im_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  conversation_type TINYINT NOT NULL COMMENT '1单聊 2群聊 3频道',
  seq_no BIGINT NOT NULL COMMENT '会话内递增序号',
  client_msg_id VARCHAR(64) NOT NULL COMMENT '客户端消息ID(幂等)',
  from_user_id BIGINT NOT NULL,
  to_user_id BIGINT DEFAULT NULL COMMENT '单聊时使用',
  group_id BIGINT DEFAULT NULL COMMENT '群聊时使用',
  msg_type TINYINT NOT NULL COMMENT '1文本 2贴纸 3图片 4GIF 5文件 6系统 7语音',
  content TEXT DEFAULT NULL,
  extra_json JSON DEFAULT NULL COMMENT '扩展数据(撤回/编辑/转发/语音/提及等)',
  file_id BIGINT DEFAULT NULL,
  send_status TINYINT NOT NULL DEFAULT 1 COMMENT '1成功 2失败 3撤回',
  sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_from_client_msg (from_user_id, client_msg_id),
  UNIQUE KEY uk_conversation_seq (conversation_id, seq_no),
  KEY idx_conversation_time (conversation_id, sent_at)
);
```

**extra_json 字段结构示例：**

```json
{
  "edited": true,
  "editedAt": "2026-05-01T10:30:00",
  "replySource": {
    "sourceMessageId": 12345,
    "sourceConversationId": 100,
    "sourceSenderId": 10001,
    "sourceMsgType": "TEXT",
    "sourcePreview": "原始消息内容"
  },
  "mentions": [
    { "userId": 10002, "displayName": "Bob" }
  ],
  "voice": {
    "duration": 15.2,
    "waveform": [0.1, 0.3, 0.5, ...]
  },
  "selfDestructSeconds": 30
}
```

### 3.2 im_conversation_user

会话成员表，记录每个用户在每个会话中的状态。

```sql
CREATE TABLE im_conversation_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  unread_count INT NOT NULL DEFAULT 0,
  last_read_seq BIGINT NOT NULL DEFAULT 0 COMMENT '最后已读序号',
  is_top TINYINT NOT NULL DEFAULT 0,
  is_mute TINYINT NOT NULL DEFAULT 0,
  is_archived TINYINT NOT NULL DEFAULT 0,
  manual_unread TINYINT NOT NULL DEFAULT 0,
  draft_content TEXT DEFAULT NULL COMMENT '草稿内容',
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_conversation_user (conversation_id, user_id)
);
```

### 3.3 im_message_receipt

消息回执表，支持送达和已读两种状态。

```sql
CREATE TABLE im_message_receipt (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  message_id BIGINT NOT NULL,
  conversation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  receipt_type TINYINT NOT NULL COMMENT '1送达 2已读',
  receipt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_message_user_receipt (message_id, user_id, receipt_type)
);
```

## 4. ID 生成规则

| 实体 | 格式 | 示例 |
|------|------|------|
| 用户编号 user_no | `U` + 10 位随机数字 | `U3829104756` |
| 群组编号 group_no | `G` + 10 位随机数字 | `G5418203967` |
| 会话编号 conversation_no | UUID v4 | `fb92ac3b-6f29-4413-89f9-3fa793d7922f` |

由 `IdGenerator` 工具类统一生成，保证唯一性。

## 5. 索引策略

- 会话消息查询：`(conversation_id, sent_at)` 复合索引
- 会话列表排序：`(last_message_time)` 索引
- 用户会话查询：`(user_id, is_archived, is_top)` 复合索引
- 消息回执查询：`(message_id, user_id, receipt_type)` 唯一索引
- 全局搜索：使用 `LIKE` 查询，依赖 `content` 字段全文扫描

## 6. 初始数据

`initdata.sql` 包含：
- 1 个管理员账号（admin / EchoIM@Admin2026!）
- 6 个演示用户（alice/bob/charlie/david/eve/frank，密码 123456）
- 示例会话和消息数据

## 7. Redis 使用

| Key 模式 | 用途 |
|----------|------|
| `echoim:online:user:{userId}` | 用户在线状态 |
| `echoim:route:user:{userId}` | 用户 WS 路由 |
| `echoim:conversation:unread:{userId}` | 未读计数缓存 |
| `echoim:config:{configKey}` | 系统配置缓存 |
| `echoim:sensitive:words` | 敏感词缓存 |
| `TOTP_CHALLENGE:{ticket}` | TOTP 登录挑战状态 |
| `TOTP_SETUP:{userId}` | TOTP 设置状态 |
