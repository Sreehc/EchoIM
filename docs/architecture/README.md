# EchoIM 系统架构设计

## 1. 项目概述

EchoIM 是一个基于前后端分离架构的网页版即时通讯系统，支持单聊、群聊、频道广播三种会话形式，提供实时消息、语音/视频通话、文件共享等核心能力。

## 2. 技术栈

| 层级 | 技术选型 | 版本 |
|------|---------|------|
| 后端框架 | Spring Boot | 3.x |
| ORM | MyBatis-Plus | 3.5+ |
| 实时通信 | Netty WebSocket | 4.1+ |
| 缓存 | Redis | 7.x |
| 数据库 | MySQL | 8.4 |
| TOTP | dev.samstevens.totp | 2.7.1 |
| 前端框架 | Vue 3 (Composition API) + TypeScript | 3.5+ |
| 状态管理 | Pinia | 2.3+ |
| UI 组件 | Element Plus | 2.9+ |
| 构建工具 | Vite | 6.x |
| 通话 | WebRTC | 原生 API |
| 部署 | Docker Compose + Nginx | - |

## 3. 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │   echoim-web     │  │   echoim-admin   │                 │
│  │  (用户端 Vue 3)   │  │  (管理后台 Vue 3) │                 │
│  └────────┬─────────┘  └────────┬─────────┘                 │
│           │ HTTP/WS             │ HTTP                       │
└───────────┼─────────────────────┼───────────────────────────┘
            │                     │
┌───────────┼─────────────────────┼───────────────────────────┐
│           ▼                     ▼         服务端层           │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Nginx (反向代理)                         │    │
│  │  / → 静态文件  /api → :8080  /ws → :8091             │    │
│  └────────┬──────────────────────┬─────────────────────┘    │
│           │                      │                          │
│  ┌────────▼──────────┐  ┌───────▼───────────┐              │
│  │  Spring Boot HTTP  │  │  Netty WebSocket  │              │
│  │  REST API (:8080)  │  │  实时消息 (:8091)  │              │
│  │                    │  │                   │              │
│  │  Controller 层     │  │  ImTextFrame-     │              │
│  │  Service 层        │  │  Handler          │              │
│  │  Mapper 层         │  │  ImSessionManager │              │
│  └────────┬───────────┘  └───────┬───────────┘              │
│           │                      │                          │
│  ┌────────▼──────────────────────▼─────────────────────┐    │
│  │                    数据层                            │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │    │
│  │  │  MySQL   │  │  Redis   │  │  OSS / 本地存储   │  │    │
│  │  │  8.4     │  │  7.x     │  │  文件资源         │  │    │
│  │  └──────────┘  └──────────┘  └──────────────────┘  │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## 4. 后端架构

### 4.1 分层结构

```
com.echoim.server
├── config/                    # 配置类
│   ├── AuthProperties         # 认证配置
│   ├── CallProperties         # 通话 ICE 配置
│   ├── FileProperties         # 文件上传配置
│   ├── ImProperties           # IM 服务配置
│   ├── JwtProperties          # JWT 配置
│   └── OssConfig              # OSS 存储配置
├── common/                    # 公共组件
│   ├── annotation/            # 自定义注解 (@RequireLogin)
│   ├── auth/                  # 登录用户上下文 (LoginUserContext)
│   ├── audit/                 # 审计日志
│   ├── constant/              # 错误码常量
│   ├── exception/             # 业务异常 (BizException)
│   ├── log/                   # 请求日志过滤器
│   ├── ratelimit/             # 接口限流
│   ├── trace/                 # 链路追踪 (traceId)
│   └── util/                  # 工具类 (ContentSanitizer, IdGenerator)
├── controller/                # REST API 控制器
│   ├── AuthController         # 认证 (登录/注册/刷新/TOTP)
│   ├── UserController         # 用户资料
│   ├── FriendController       # 好友管理
│   ├── ConversationController # 会话管理
│   ├── MessageController      # 消息操作 (撤回/编辑/转发/置顶/已读详情)
│   ├── GroupController        # 群组管理
│   ├── SearchController       # 全局搜索
│   ├── FileController         # 文件上传/下载
│   ├── CallController         # 通话管理
│   ├── BlockController        # 用户屏蔽
│   ├── ReportController       # 举报
│   ├── ScheduledMessageController # 定时消息
│   ├── OfflineSyncController  # 离线同步
│   ├── HealthController       # 健康检查
│   ├── MonitoringController   # WS/JVM 监控
│   └── admin/                 # 管理后台 API
│       ├── AdminAuthController
│       ├── AdminDashboardController
│       ├── AdminUserController
│       ├── AdminGroupController
│       ├── AdminReportController
│       ├── AdminNoticeController
│       ├── AdminBanController
│       ├── AdminSensitiveWordController
│       ├── AdminOperationLogController
│       ├── AdminBeautyNoController
│       ├── AdminConfigController
│       └── AdminVersionController
├── dto/                       # 请求参数对象
├── vo/                        # 响应视图对象
├── entity/                    # 数据库实体 (MyBatis-Plus)
├── mapper/                    # MyBatis Mapper 接口
├── service/                   # 业务逻辑层
│   ├── auth/                  # 认证服务 (AuthService, TotpService)
│   ├── block/                 # 屏蔽服务
│   ├── call/                  # 通话服务 (CallService)
│   ├── config/                # 系统配置服务
│   ├── conversation/          # 会话服务
│   ├── file/                  # 文件服务 (含 OSS/本地存储策略)
│   ├── friend/                # 好友服务
│   ├── group/                 # 群组服务
│   ├── message/               # 消息服务 (Command/View/Scheduled)
│   ├── search/                # 搜索服务
│   ├── sensitive/             # 敏感词服务
│   ├── admin/                 # 管理后台服务 (Dashboard, Report)
│   └── impl/                  # 服务实现
├── im/                        # IM 核心模块
│   ├── netty/                 # Netty 服务端
│   │   ├── ImServerBootstrap  # 启动器
│   │   ├── ImServerChannelInitializer
│   │   └── ImTextFrameHandler # WebSocket 消息处理器
│   ├── session/               # 会话管理 (ImSessionManager)
│   ├── service/               # IM 服务
│   │   ├── ImSingleChatService
│   │   ├── ImGroupChatService
│   │   ├── ImWsPushService    # WS 推送
│   │   ├── ImOnlineService    # 在线状态
│   │   └── OfflineSyncService
│   ├── model/                 # WS 消息模型
│   │   ├── WsMessage          # 信封格式
│   │   ├── WsMessageType      # 消息类型枚举 (27 种)
│   │   └── Ws*Data            # 各类型数据体
│   ├── monitor/               # WS 指标 (WsMetrics)
│   └── ws/                    # WS 连接查询 (WsConnectController)
└── interceptor/               # HTTP 拦截器 (LoginInterceptor)
```

### 4.2 请求处理流程

**HTTP API 请求：**
```
Client → Nginx → LoginInterceptor (JWT 校验)
  → @RequireLogin 注解检查
  → RateLimitInterceptor (限流)
  → Controller → Service → Mapper → MySQL
  → ApiResponse 统一响应
```

**WebSocket 消息：**
```
Client → Nginx → Netty :8091
  → ImServerChannelInitializer (握手)
  → ImTextFrameHandler (消息分发)
    → AUTH: JWT 校验，绑定 session
    → CHAT_SINGLE: ImSingleChatService
    → CHAT_GROUP: ImGroupChatService
    → CALL_*: CallService
    → ACK/READ: 回执处理
  → ImWsPushService (推送目标用户)
  → ImSessionManager (在线状态)
```

### 4.3 认证机制

- **JWT + Refresh Token**：登录返回 accessToken（短有效期）和 refreshToken（长有效期）
- **邮箱验证码**：注册和登录时发送 6 位验证码
- **TOTP 两步验证**：基于 TOTP 标准，支持 Google Authenticator
- **可信设备**：设备指纹 + 授权令牌，免重复验证
- **密码找回**：邮箱验证码 → 重置密码

### 4.4 消息存储模型

消息通过 `extraJson`（JSON 字段）存储扩展数据，避免频繁 DDL 变更：

- `recalled` / `recalledAt`：撤回标记
- `edited` / `editedAt`：编辑标记
- `forwardSource`：转发来源
- `replySource`：回复来源
- `sticker`：贴纸数据
- `voice`：语音数据（时长 + 波形）
- `mentions`：@提及列表
- `selfDestructSeconds`：阅后即焚时长

### 4.5 文件存储

支持两种存储策略，通过 `ECHOIM_FILE_STORAGE_TYPE` 配置：

- **OSS**（默认）：阿里云 OSS，支持签名 URL 下载
- **本地**：`LocalFileStorageService`，文件存储在本地磁盘

图片上传时自动生成 300px 宽缩略图（`_thumb.jpg`）。

## 5. 前端架构

### 5.1 用户端 (echoim-web)

```
src/
├── adapters/          # API 响应 → 前端类型适配层
├── components/
│   └── chat/          # 聊天相关组件
│       ├── MessageBubble.vue       # 消息气泡
│       ├── MessagePane.vue         # 消息列表
│       ├── MessageComposer.vue     # 消息编辑器
│       ├── ChatTopbar.vue          # 聊天顶栏
│       ├── ConversationSidebar.vue # 会话侧边栏
│       ├── ConversationListItem.vue
│       ├── CallOverlay.vue         # 通话浮层
│       ├── ImageViewer.vue         # 图片查看器
│       ├── VoiceRecorder.vue       # 语音录制
│       ├── VoicePlayer.vue         # 语音播放
│       ├── MentionSelector.vue     # @提及选择器
│       ├── PinnedMessagesBanner.vue
│       ├── ScheduledMessagesPanel.vue
│       ├── MessageReadDetails.vue  # 已读详情
│       └── ...
├── composables/       # 组合式函数 (useMediaRecorder, useVirtualScroll)
├── services/          # API 调用层
│   ├── http.ts        # HTTP 请求封装 (fetch + JWT + 401 刷新)
│   ├── ws.ts          # WebSocket 客户端 (EchoWsClient)
│   ├── auth.ts / messages.ts / conversations.ts / groups.ts / ...
├── stores/            # Pinia 状态管理
│   ├── auth.ts        # 认证状态 (多账号切换)
│   ├── chat.ts        # 聊天核心状态 (会话列表/消息/在线状态)
│   ├── call.ts        # 通话状态 (WebRTC 连接管理)
│   └── ui.ts          # UI 状态 (主题/偏好/面板)
├── types/             # TypeScript 类型定义
│   ├── chat.ts        # 业务类型 (ChatMessage, ConversationSummary, ...)
│   └── api.ts         # API 响应类型
├── utils/             # 工具函数
│   ├── format.ts      # 时间格式化/高亮
│   ├── browserNotifications.ts
│   └── storage.ts
├── views/
│   ├── LoginView.vue          # 登录/注册/TOTP
│   ├── PublicProfileView.vue  # 公开主页
│   ├── InviteView.vue         # 邀请链接加入
│   └── chat/
│       └── ChatHomeView.vue   # 主聊天页面 (核心页面，~2500 行)
└── router/            # 路由配置
```

### 5.2 管理后台 (echoim-admin)

独立项目，Vue 3 + Element Plus + Pinia，端口 5174，API 代理到 `/admin/*`。

```
src/
├── api/               # API 调用 (axios 封装)
├── views/
│   ├── dashboard/     # 数据看板
│   ├── user/          # 用户管理
│   ├── group/         # 群组管理
│   ├── report/        # 举报管理
│   ├── sensitive-word/# 敏感词管理
│   ├── notice/        # 系统公告
│   ├── ban/           # 用户封禁
│   ├── operation-log/ # 操作日志
│   ├── beauty-no/     # 靓号管理
│   ├── config/        # 系统配置
│   └── version/       # 版本管理
├── layouts/
│   └── AdminLayout.vue # 侧边栏 + 顶部栏布局
├── router/            # 路由 + 鉴权守卫
└── stores/            # 管理员认证状态
```

## 6. 数据流

### 6.1 单聊消息发送

```
1. 前端 MessageComposer → stores/chat.ts sendMessageThroughRealtime()
2. EchoWsClient.send({ type: CHAT_SINGLE, data: { conversationId, toUserId, msgType, content } })
3. Netty ImTextFrameHandler → ImSingleChatService.sendSingle()
4. 校验权限 → 敏感词过滤 → 生成 seqNo → 写入 im_message
5. 更新会话预览 → 推送给接收方 (CONVERSATION_CHANGE + CHAT_SINGLE)
6. 推送给发送方 (ACK: SUCCESS)
```

### 6.2 离线消息同步

```
1. 用户上线 → AUTH 成功后触发 OfflineSyncService
2. 查询所有会话的 lastReadSeq 与最新 seqNo 的差值
3. 批量拉取缺失消息 → OFFLINE_SYNC 推送给客户端
4. 客户端 stores/chat.ts 处理 OFFLINE_SYNC，补充消息列表
```

## 7. 安全机制

- **XSS 防护**：`ContentSanitizer.escapeHtml()` 处理消息内容
- **接口限流**：`@RateLimit` 注解 + `LocalRateLimitService`（基于 ConcurrentHashMap）
- **文件安全**：Tika 内容类型检测 + 扩展名白名单 + ImageIO 图片验证
- **敏感词过滤**：`SensitiveWordService` 缓存式词库，拦截/标记两级策略
- **链路追踪**：`TraceIdFilter` MDC 注入 traceId，慢请求告警（>3s）
- **请求日志**：`RequestLoggingFilter` 记录 method/path/status/duration/userId

## 8. 可观测性

- **健康检查**：`GET /api/health` — 数据库 `Connection.isValid()` + Redis `ping()` + WS 在线数
- **WS 指标**：`GET /api/monitor/ws` — 消息收发计数、连接开关计数、鉴权失败计数
- **JVM 指标**：`GET /api/monitor/jvm` — 堆内存、线程数、可用处理器
- **错误上报**：前端全局错误采集 → `POST /api/errors/report` → 结构化日志
