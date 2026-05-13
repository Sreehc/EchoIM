# EchoIM 接口文档

## 1. 基础约定

### 1.1 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| HTTP API | 8080 | REST API |
| WebSocket | 8091 | 实时消息 |

### 1.2 鉴权方式

- HTTP：`Authorization: Bearer {accessToken}`
- WebSocket：连接后首条消息发送 `AUTH` 类型完成认证

### 1.3 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "202605011230001234",
  "traceId": "abc123"
}
```

错误码：`0` 成功，`4xxxx` 客户端错误，`5xxxx` 服务端错误。

### 1.4 分页格式

```json
{
  "list": [],
  "pageNo": 1,
  "pageSize": 20,
  "total": 100
}
```

---

## 2. 认证模块 `/api/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册（username + email + code） |
| POST | `/api/auth/login` | 登录（username + password） |
| POST | `/api/auth/login/challenge/verify` | 邮箱验证码验证 |
| POST | `/api/auth/login/challenge/resend` | 重发验证码 |
| POST | `/api/auth/login/totp/verify` | TOTP 验证 |
| POST | `/api/auth/trusted-devices/login` | 受信设备登录 |
| POST | `/api/auth/refresh` | 刷新 Token |
| POST | `/api/auth/logout` | 登出 |
| POST | `/api/auth/change-password` | 修改密码 |
| POST | `/api/auth/email/send-bind-code` | 发送绑定邮箱验证码 |
| POST | `/api/auth/email/bind` | 绑定邮箱 |
| POST | `/api/auth/recovery/send-code` | 发送找回验证码 |
| POST | `/api/auth/recovery/verify-code` | 验证找回码 |
| POST | `/api/auth/recovery/reset-password` | 重置密码 |
| GET | `/api/auth/trusted-devices` | 受信设备列表 |
| DELETE | `/api/auth/trusted-devices/{id}` | 移除受信设备 |
| GET | `/api/auth/security-events` | 安全事件列表 |
| GET | `/api/auth/totp/status` | TOTP 状态 |
| POST | `/api/auth/totp/setup` | 初始化 TOTP |
| POST | `/api/auth/totp/enable` | 启用 TOTP |
| POST | `/api/auth/totp/disable` | 禁用 TOTP |

---

## 3. 用户模块 `/api/user`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/me` | 当前用户资料 |
| PUT | `/api/user/me` | 更新资料 |
| GET | `/api/user/check-username` | 检查用户名可用性 |
| GET | `/api/user/search` | 搜索用户 |
| GET | `/api/user/{id}/profile` | 公开资料（含屏蔽状态） |
| GET | `/api/user/u/{username}/page` | 公开主页 |

---

## 4. 好友模块 `/api/friends`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/friends` | 好友列表 |
| POST | `/api/friends/request` | 发送好友申请 |
| GET | `/api/friends/requests` | 好友申请列表 |
| PUT | `/api/friends/requests/{id}` | 处理申请（同意/拒绝） |
| DELETE | `/api/friends/{id}` | 删除好友 |
| PUT | `/api/friends/{id}/remark` | 修改好友备注 |
| POST | `/api/friends/{id}/block` | 拉黑好友 |
| POST | `/api/friends/{id}/unblock` | 取消拉黑 |

---

## 5. 会话模块 `/api/conversations`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/conversations` | 会话列表（含未读数/草稿/置顶/免打扰） |
| GET | `/api/conversations/{id}` | 会话详情 |
| GET | `/api/conversations/{id}/messages` | 消息分页（cursor 或 offset） |
| GET | `/api/conversations/{id}/files` | 会话文件列表 |
| PUT | `/api/conversations/{id}/top` | 置顶/取消置顶 |
| PUT | `/api/conversations/{id}/mute` | 免打扰/取消免打扰 |
| PUT | `/api/conversations/{id}/archive` | 归档/取消归档 |
| PUT | `/api/conversations/{id}/unread` | 标记未读/已读 |
| PUT | `/api/conversations/{id}/draft` | 保存草稿 |
| GET | `/api/conversations/{id}/draft` | 加载草稿 |
| DELETE | `/api/conversations/{id}` | 删除会话 |

---

## 6. 消息模块 `/api/messages`

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/api/messages/{id}/recall` | 撤回消息 |
| PUT | `/api/messages/{id}/edit` | 编辑消息 |
| DELETE | `/api/messages/{id}` | 删除并撤回（双向） |
| POST | `/api/messages/forward` | 转发消息 |
| PUT | `/api/messages/{id}/reaction` | 表情回应 |
| PUT | `/api/messages/{id}/pin` | 置顶消息 |
| PUT | `/api/messages/{id}/unpin` | 取消置顶 |
| GET | `/api/messages/pinned?conversationId=` | 置顶消息列表 |
| GET | `/api/messages/{id}/receipts` | 群消息已读详情 |

---

## 7. 定时消息 `/api/scheduled-messages`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/scheduled-messages` | 创建定时消息 |
| GET | `/api/scheduled-messages?conversationId=` | 定时消息列表 |
| DELETE | `/api/scheduled-messages/{id}` | 取消定时消息 |
| POST | `/api/scheduled-messages/{id}/send-now` | 立即发送 |

---

## 8. 群组模块 `/api/groups`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/groups` | 创建群组 |
| GET | `/api/groups/{id}` | 群组详情 |
| PUT | `/api/groups/{id}` | 更新群组（名称/公告） |
| DELETE | `/api/groups/{id}` | 解散群组 |
| GET | `/api/groups/{id}/members` | 成员列表 |
| POST | `/api/groups/{id}/members` | 添加成员 |
| DELETE | `/api/groups/{id}/members/{userId}` | 移除成员 |
| DELETE | `/api/groups/{id}/members/me` | 退出群组 |
| PUT | `/api/groups/{id}/members/{userId}/role` | 变更角色 |
| PUT | `/api/groups/{id}/members/{userId}/mute` | 禁言 |
| DELETE | `/api/groups/{id}/members/{userId}/mute` | 解除禁言 |
| POST | `/api/groups/{id}/invites` | 创建邀请链接 |
| GET | `/api/groups/{id}/invites` | 邀请链接列表 |
| DELETE | `/api/groups/{id}/invites/{inviteId}` | 撤销邀请链接 |
| GET | `/api/groups/invite/{token}/preview` | 邀请预览（公开） |
| POST | `/api/groups/invite/{token}/join` | 通过链接加入（公开） |
| POST | `/api/groups/{id}/join-requests` | 提交入群申请 |
| GET | `/api/groups/{id}/join-requests` | 待审批列表 |
| PUT | `/api/groups/{id}/join-requests/{requestId}` | 审批（同意/拒绝） |

---

## 9. 搜索模块 `/api/search`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/search/global` | 全局搜索 |

**参数：**
- `keyword`：搜索关键词
- `conversationLimit`：会话结果上限（默认 8）
- `userLimit`：用户结果上限（默认 8）
- `messageLimit`：消息结果上限（默认 12）
- `msgType`：消息类型筛选（IMAGE/FILE/VOICE/GIF）
- `dateFrom`：开始日期（ISO 8601）
- `dateTo`：结束日期（ISO 8601）

---

## 10. 通话模块 `/api/calls`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/calls` | 创建通话 |
| GET | `/api/calls/{id}` | 通话详情 |

---

## 11. 文件模块 `/api/files`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/files/upload` | 上传文件 |
| GET | `/api/files/{id}/download` | 下载文件（签名 URL） |

---

## 12. 屏蔽模块 `/api/blocks`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/blocks` | 屏蔽用户 |
| DELETE | `/api/blocks/{userId}` | 取消屏蔽 |
| GET | `/api/blocks` | 屏蔽列表 |

---

## 13. 举报模块 `/api/reports`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/reports` | 提交举报（targetType: 1消息/2用户, reason, description） |

---

## 14. 系统公告 `/api/notices`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/notices?pageNo=1&pageSize=20` | 获取当前用户可见公告列表（包含 `unreadCount`） |
| PUT | `/api/notices/{noticeId}/read` | 标记公告已读 |

---

## 15. 离线同步 `/api/offline-sync`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/offline-sync` | 拉取离线消息 |

---

## 16. 运维接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查（DB + Redis + WS） |
| GET | `/api/monitor/ws` | WebSocket 指标 |
| GET | `/api/monitor/jvm` | JVM 指标 |
| POST | `/api/errors/report` | 前端错误上报 |

---

## 17. 管理后台 API `/api/admin/*`

### 17.1 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/auth/login` | 管理员登录 |
| POST | `/api/admin/auth/logout` | 管理员退出 |

### 17.2 数据看板

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/dashboard/overview` | 核心指标（总用户/今日新增/总消息/今日消息/在线） |
| GET | `/api/admin/dashboard/message-trend?days=7` | 消息趋势 |
| GET | `/api/admin/dashboard/user-trend?days=7` | 用户趋势 |
| GET | `/api/admin/dashboard/message-types` | 消息类型分布 |
| GET | `/api/admin/dashboard/online-stats` | 实时在线统计 |

### 17.3 系统公告

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/notices` | 公告列表（支持按状态分页） |
| POST | `/api/admin/notices` | 发布公告（全员或指定用户） |
| PUT | `/api/admin/notices/{id}/withdraw` | 撤回公告 |

### 16.3 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users` | 用户列表 |
| PUT | `/api/admin/users/{id}/status` | 启用/禁用 |
| PUT | `/api/admin/users/{id}/offline` | 强制下线 |

### 16.4 群组管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/groups` | 群组列表 |
| DELETE | `/api/admin/groups/{id}` | 解散群组 |

### 16.5 举报管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/reports` | 举报列表（按状态筛选） |
| PUT | `/api/admin/reports/{id}/handle` | 处理举报（action: 1忽略/2警告/3禁言/4封号） |

### 16.6 敏感词管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/sensitive-words` | 敏感词列表 |
| POST | `/api/admin/sensitive-words` | 添加敏感词 |
| DELETE | `/api/admin/sensitive-words/{id}` | 删除敏感词 |
| POST | `/api/admin/sensitive-words/reload` | 刷新缓存 |

### 16.7 系统公告

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/notices` | 公告列表 |
| POST | `/api/admin/notices` | 发布公告 |
| PUT | `/api/admin/notices/{id}/withdraw` | 撤回公告 |

### 16.8 用户封禁

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/bans` | 封禁历史 |
| POST | `/api/admin/bans` | 封禁用户（banMinutes: null=永久） |
| PUT | `/api/admin/bans/{id}/unban` | 解除封禁 |

### 16.9 操作日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/operation-logs` | 操作日志（按 moduleName/adminUserId 筛选） |

### 16.10 靓号/配置/版本

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST/DELETE | `/api/admin/beauty-nos` | 靓号 CRUD |
| GET/POST/PUT | `/api/admin/configs` | 系统配置 CRUD |
| GET/POST/PUT | `/api/admin/versions` | 版本 CRUD |

---

## 17. WebSocket 协议

### 17.1 连接

```
ws://host:8091/ws?token={accessToken}
```

### 17.2 消息格式

```json
{
  "type": "CHAT_SINGLE",
  "traceId": "optional-trace-id",
  "clientMsgId": "optional-client-msg-id",
  "timestamp": 1714567890000,
  "data": { ... }
}
```

### 17.3 消息类型

| 类型 | 方向 | 说明 |
|------|------|------|
| `AUTH` | C→S | 认证（data: { token }） |
| `PING` / `PONG` | 双向 | 心跳 |
| `CHAT_SINGLE` | C→S→C | 单聊消息 |
| `CHAT_GROUP` | C→S→C | 群聊消息 |
| `ACK` | S→C | 送达确认（含发送结果） |
| `READ` | C→S | 已读回执 |
| `MESSAGE_RECALL` | S→C | 消息撤回通知 |
| `MESSAGE_EDIT` | S→C | 消息编辑通知 |
| `MESSAGE_DELETE` | S→C | 消息删除通知 |
| `MESSAGE_PIN` / `MESSAGE_UNPIN` | S→C | 置顶变更 |
| `CONVERSATION_CHANGE` | S→C | 会话变更（含 @提及） |
| `CALL_INVITE` / `ACCEPT` / `REJECT` / `CANCEL` / `END` | 双向 | 通话信令 |
| `CALL_OFFER` / `ANSWER` / `ICE_CANDIDATE` | 双向 | WebRTC 信令透传 |
| `CALL_STATE` | S→C | 通话状态同步 |
| `TYPING` | C→S→C | 输入中状态 |
| `USER_ONLINE` / `USER_OFFLINE` | S→C | 在线状态变更 |
| `FORCE_OFFLINE` | S→C | 强制下线 |
| `OFFLINE_SYNC` | S→C | 离线消息补发 |
| `NOTICE` | S→C | 系统通知 |
