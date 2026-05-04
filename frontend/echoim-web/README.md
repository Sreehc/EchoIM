# echoim-web

EchoIM 用户端前端，基于 Vue 3 + TypeScript + Pinia + Element Plus 构建。

## 技术栈

- **框架**：Vue 3 (Composition API) + TypeScript
- **状态管理**：Pinia
- **UI 组件**：Element Plus
- **构建工具**：Vite
- **实时通信**：WebSocket（原生 API）
- **通话**：WebRTC

## 项目结构

```
src/
├── adapters/       # API 响应数据适配层
├── assets/         # 静态资源（图标、贴纸 SVG）
├── components/     # 通用组件
├── composables/    # 组合式函数
├── router/         # Vue Router 路由配置
├── stores/         # Pinia 状态管理
│   ├── auth.ts     # 认证状态
│   ├── chat.ts     # 聊天核心状态
│   ├── call.ts     # 语音通话状态
│   └── ...
├── styles/         # 全局样式与主题变量
├── types/          # TypeScript 类型定义
├── utils/          # 工具函数
└── views/
    ├── LoginView.vue
    ├── PublicProfileView.vue
    └── chat/
        └── ChatHomeView.vue
```

## 已实现功能

### 消息与会话
- 单聊、群聊、频道三种会话类型
- 文本、图片、文件、GIF、贴纸、语音消息发送
- 消息撤回、编辑、回复、转发
- 消息已读回执与送达状态，群消息已读详情对话框
- 表情回应（Reaction）
- 离线消息同步（上线自动拉取未读）
- 输入状态实时提示
- 消息定时发送（日期时间选择器 + 待发送列表面板）
- 消息草稿自动保存（localStorage + 后端同步）
- 阅后即焚（自毁倒计时 + 自动撤回）
- 消息置顶（右键菜单 + 置顶横幅）
- 举报消息（原因选择对话框）

### 多媒体
- 语音消息录制（MediaRecorder + 波形可视化）与播放
- 视频通话（全屏远程画面 + 本地画中画 + 摄像头开关）
- 图片缩略图预览、全屏查看器（缩放 / 滑动 / 下载 / 转发）
- 多图批量发送

### 会话管理
- 会话置顶、免打扰、归档
- 手动标记未读
- 会话文件夹（收件箱、归档、未读、单聊、群聊、频道）
- Saved Messages（收藏 / 自对话）
- 右键会话菜单
- 会话文件列表查看

### 社交功能
- 好友申请、好友列表、好友备注
- 用户搜索
- 全局搜索（会话、用户、消息内容）+ 关键词高亮 + 消息类型筛选 + 日期范围筛选
- 个人资料编辑
- 公开主页查看
- 用户屏蔽（双向消息隔离）

### 群组治理
- 群主 / 管理员 / 普通成员角色
- 群公告、群名称编辑
- 成员管理（移除、角色变更、禁言 / 解除禁言）
- @提及（成员选择器、高亮、通知提醒）
- 群邀请链接（生成、预览、加入、管理）
- 入群审批
- 群解散

### 通话
- 一对一语音 / 视频通话（WebRTC）
- 信令协商与 ICE 交换
- 通话状态管理

### 安全
- 两步验证（TOTP）
- 可信设备管理
- 安全事件日志

### 界面与体验
- 深色 / 浅色主题切换
- 聊天偏好设置（回车发送、紧凑列表、紧凑气泡）
- 桌面通知
- 响应式布局

## 本地开发

前置条件：
- Node.js 18+
- 后端服务已启动（Spring Boot `:8080` + WebSocket `:8091`）

启动：

```bash
npm install
npm run dev
```

默认开发代理：
- `/api` -> `http://127.0.0.1:8080`
- `/ws` -> `ws://127.0.0.1:8091`

前端只请求同源相对路径（如 `/api/auth/login`），不直接写后端完整地址。开发环境依赖 Vite proxy，生产环境依赖 Nginx 反向代理。

## 生产构建

```bash
npm run build
```

构建产物输出到 `dist/` 目录，使用 Nginx 托管静态文件并反向代理 API 和 WebSocket。Nginx 配置示例见 `deploy/nginx/echoim.conf`。

推荐部署方式：前端静态资源、HTTP API、WebSocket 挂在同一域名下：

- `https://im.example.com/` -> 前端静态文件
- `https://im.example.com/api/...` -> Spring Boot `:8080`
- `wss://im.example.com/ws` -> IM WebSocket `:8091`

## 校验命令

```bash
npm run typecheck    # TypeScript 类型检查
npm run build        # 生产构建
npm run test:e2e     # 端到端测试
npm run test:a11y    # 无障碍测试
```
