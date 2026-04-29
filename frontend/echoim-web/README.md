# echoim-web

EchoIM Web 聊天客户端。

当前前端已经从“静态页面阶段”进入“真实后端联调阶段”，并且开始按 Telegram 常用能力做产品化补齐。

相关总文档：

- [开发文档](/Users/cheers/Desktop/workspace/EchoIM/EchoIM%20开发文档.md)
- [接口文档](/Users/cheers/Desktop/workspace/EchoIM/接口文档.md)

## 当前已接入能力

- 登录：`POST /api/auth/login`
- 退出登录：`POST /api/auth/logout`
- 会话列表：`GET /api/conversations`
- 历史消息：`GET /api/conversations/:id/messages`
- 上拉更早消息：`GET /api/conversations/:id/messages?maxSeqNo=...`
- 已读 / 置顶 / 免打扰 / 删除：`PUT /read`、`PUT /top`、`PUT /mute`、`DELETE /api/conversations/:id`
- 单聊 / 群聊详情：`GET /api/users/:id`、`GET /api/groups/:id`
- 消息撤回 / 编辑：`PUT /api/messages/:id/recall`、`PUT /api/messages/:id/edit`
- 实时链路：`GET /api/im/info` + `WebSocket /ws`
- 离线补回：`POST /api/offline-sync/messages`

## 当前前端状态说明

### 已完成

- 单聊 / 群聊 / 频道型会话基础界面
- 会话列表与聊天主区联动
- 会话详情抽屉
- 会话内消息搜索
- 文本消息发送、编辑、撤回
- 已读、送达、重连、离线补回
- 置顶、免打扰、删除
- 深浅主题、聊天偏好、桌面通知
- 右键会话菜单

### 部分完成

- `Mark as unread`
  当前是前端本地行为，刷新后不保留。
- `Archive`
  当前是前端本地隐藏，不是真正归档。
- 图片消息
  已有消息类型，但展示体验还不够完整。
- 文件消息
  已有卡片展示，但下载 / 预览链路需要补强。
- 新建私聊 / 群组 / 频道
  入口存在，真实创建流程还未完成。

## 产品优先级 PRD 清单

### P0

- 真实归档
- 真实标记未读
- 新建私聊 / 群组 / 频道完整创建流程
- 图片消息真实展示
- 文件消息下载 / 预览增强
- 回复 / 引用消息

### P1

- 转发消息
- Saved Messages
- 全局搜索
- 联系人闭环
- 会话文件夹

### P2

- 表情 / Sticker / GIF
- reaction
- 语音消息
- 多账号
- 用户名公开体系 / 分享链接

## 前端任务拆分表

### 第一阶段：补真能力

| 模块 | 任务 | 输出 |
|---|---|---|
| 会话列表 | 接入真实归档状态 | 会话筛选、归档显隐、状态持久化 |
| 会话列表 | 接入真实标记未读 | 右键菜单、角标、多端一致 |
| 新建流程 | 完成新建私聊 / 群组 / 频道 | 弹层、表单、创建后跳转 |
| 消息区 | 图片消息真实展示 | 缩略图、查看大图、失败态 |
| 消息区 | 文件消息增强 | 下载、基础预览、错误提示 |
| 输入区 | 回复 / 引用 | 回复条、发送、消息展示、跳转 |

### 第二阶段：补高频聊天能力

| 模块 | 任务 | 输出 |
|---|---|---|
| 消息区 | 转发消息 | 转发入口、选择目标会话、发送结果 |
| 会话区 | Saved Messages | 内置自用会话、列表固定展示 |
| 搜索 | 全局搜索 | 会话 / 联系人 / 消息搜索页 |
| 联系人 | 联系人闭环 | 搜索、申请、通过、资料联动 |

### 第三阶段：补组织能力

| 模块 | 任务 | 输出 |
|---|---|---|
| 会话列表 | 会话文件夹 | 文件夹切换、规则筛选、管理入口 |
| 群组 / 频道 | 角色与治理 | 成员列表、角色展示、基础管理动作 |
| 会话列表 | 批量管理 | 批量已读、归档、静音、删除 |

## 本地联调

前置条件：

- MySQL 已启动，并存在 `echoim` 数据库与演示账号种子数据
- Redis 已启动
- `ECHOIM_JWT_SECRET` 必须满足 HMAC 长度要求，建议至少 32 字节
- 如果本地没有 OSS，后端需设置 `ECHOIM_FILE_STORAGE_TYPE=local`

启动步骤：

```bash
npm install
npm run dev
```

默认开发代理：

- `/api -> http://127.0.0.1:8080`
- `/ws -> ws://127.0.0.1:8091`

## 校验命令

```bash
npm run typecheck
npm run build
npm run test:e2e
npm run test:a11y
```

`test:e2e` 当前覆盖：

- 单聊发送、已读、断线重连与离线补回
- 消息编辑、撤回与退出登录
- 基础布局、设置面板、资料编辑、桌面通知提示

## 评审建议

前端评审时，不要只看“页面像不像 Telegram”，而要优先检查：

1. 状态是否真实持久化
2. 会话和消息在刷新、多端、重连后是否一致
3. 新功能是否已经具备真实后端闭环
