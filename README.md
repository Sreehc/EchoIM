# EchoIM

EchoIM 是一套完整的即时通讯系统，支持单聊、群聊、频道广播等多种会话形式，提供实时消息、语音通话、文件共享等核心能力。项目采用 Java Spring Boot + Netty 构建后端服务，Vue 3 + TypeScript 构建前端应用，配套独立的管理后台用于系统运维。

## 功能特性

### 即时通讯
- 单聊（1v1 私信）、群聊（多人会话）、频道（广播式推送）
- 实时消息收发，基于 WebSocket 长连接
- 文本、图片、文件、GIF、贴纸、语音等多种消息类型
- 消息撤回、编辑、回复、转发
- 表情回应（Reaction）
- 消息已读回执、送达状态，群消息已读详情
- 输入状态实时提示
- 离线消息同步，上线后自动拉取未读内容
- 消息定时发送、消息草稿自动保存与同步
- 阅后即焚（自毁消息）
- 消息置顶

### 语音 / 视频通话
- 一对一语音 / 视频通话（WebRTC）
- 信令协商、ICE 候选交换
- 通话状态管理（振铃、接听、拒接、挂断、超时）
- 视频通话支持本地画中画、摄像头开关

### 多媒体增强
- 语音消息录制与播放（波形可视化）
- 图片缩略图自动生成、全屏查看器（缩放 / 滑动 / 下载）
- 多图批量发送

### 会话管理
- 会话置顶、免打扰、归档
- 未读计数、手动标记未读
- 会话文件夹（收件箱、归档、未读、单聊、群聊、频道）
- 收藏消息（Saved Messages / 自对话）
- 会话文件列表查看

### 社交关系
- 好友申请、好友列表、好友备注
- 用户搜索、全局搜索（会话 + 用户 + 消息内容）
- 全局搜索支持关键词高亮、按消息类型筛选、按日期范围筛选
- 个人资料查看与编辑
- 公开主页
- 用户屏蔽（双向消息隔离）

### 群组治理
- 群主 / 管理员 / 普通成员角色体系
- 群公告、群名称编辑
- 成员管理（移除、角色变更、禁言 / 解除禁言）
- 群解散、频道解散
- @提及（成员选择器、高亮、通知）
- 群邀请链接（生成、预览、加入、管理）
- 入群审批
- 群消息置顶

### 管理后台
- 数据看板（核心指标概览、消息 / 用户趋势图、消息类型分布、实时在线统计）
- 用户管理（查询、封禁、资料编辑）
- 群组管理（查看、解散）
- 系统配置管理、版本管理、靓号管理
- 举报管理（审核处理：忽略 / 警告 / 禁言 / 封号）
- 敏感词管理（CRUD、缓存刷新）
- 系统公告（全员 / 指定用户发布与撤回）
- 用户封禁管理（临时 / 永久封禁、解除封禁）
- 操作日志查询

### 安全机制
- 邮箱验证码登录
- JWT 令牌认证，支持刷新令牌
- 可信设备管理、登录异常检测
- 两步验证（TOTP）
- 密码修改、账号找回
- 安全事件日志
- 敏感内容过滤（敏感词拦截 / 标记）
- 双向消息删除

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Java 17, Spring Boot 3 |
| ORM | MyBatis-Plus |
| WebSocket | Netty |
| 缓存 | Redis 7 |
| 数据库 | MySQL 8.4 |
| 前端框架 | Vue 3, TypeScript, Pinia |
| UI 组件 | Element Plus |
| 通话 | WebRTC |
| 部署 | Docker Compose, Nginx |

## 项目结构

```
EchoIM/
├── backend/                    # 后端服务
│   └── echoim-server/          # Spring Boot 主应用
│       ├── controller/         # REST API 控制器
│       │   └── admin/          # 管理后台 API
│       ├── service/            # 业务逻辑层
│       ├── entity/             # 数据库实体
│       ├── mapper/             # MyBatis Mapper
│       └── vo/                 # 视图对象
├── frontend/
│   ├── echoim-web/             # 用户端前端 (Vue 3)
│   │   └── src/
│   │       ├── views/          # 页面组件
│   │       ├── stores/         # Pinia 状态管理
│   │       ├── adapters/       # API 数据适配
│   │       └── types/          # TypeScript 类型
│   └── echoim-admin/           # 管理后台前端 (Vue 3)
│       └── src/
│           ├── views/          # 管理页面
│           └── stores/         # 状态管理
├── sql/                        # 数据库脚本
│   ├── init.sql                # 建表 DDL
│   └── initdata.sql            # 初始数据
├── deploy/                     # 部署配置
│   └── nginx/                  # Nginx 配置
├── docs/                       # 开发文档
├── docker-compose.yml          # Docker 编排
└── .env.example                # 环境变量模板
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.8+

### 1. 克隆项目

```bash
git clone https://github.com/your-username/EchoIM.git
cd EchoIM
```

### 2. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 文件，填写数据库密码、JWT 密钥、邮件服务、OSS 等配置。

### 3. 初始化数据库

```bash
mysql -u root -p < sql/init.sql
mysql -u root -p echoim < sql/initdata.sql
```

初始管理员账号：用户名 `admin`，密码 `EchoIM@Admin2026!`

### 4. 启动后端

```bash
cd backend/echoim-server
mvn spring-boot:run
```

后端默认监听 `8080`（HTTP）和 `8091`（WebSocket）。

### 5. 启动前端

```bash
cd frontend/echoim-web
npm install
npm run dev
```

用户端默认运行在 `http://localhost:5173`。

```bash
cd frontend/echoim-admin
npm install
npm run dev
```

管理后台默认运行在 `http://localhost:5174`。

## Docker 部署

使用 Docker Compose 一键启动 MySQL、Redis 和后端服务：

```bash
cp .env.example .env
# 编辑 .env 填写必要配置

docker compose up -d
```

服务启动后：
- 后端 API：`http://localhost:8080`
- WebSocket：`ws://localhost:8091`

前端项目需要单独构建部署，推荐使用 Nginx 托管静态文件并反向代理后端 API。Nginx 配置示例见 `deploy/nginx/echoim.conf`。

### Nginx 反向代理要点

```nginx
# 前端静态文件
location / {
    root /path/to/echoim-web/dist;
    try_files $uri $uri/ /index.html;
}

# 后端 API 代理
location /api/ {
    proxy_pass http://localhost:8080;
}

# WebSocket 代理
location /ws/ {
    proxy_pass http://localhost:8091;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

## 环境变量说明

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MYSQL_HOST` | MySQL 地址 | `localhost` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_DB` | 数据库名 | `echoim` |
| `MYSQL_USERNAME` | MySQL 用户名 | `root` |
| `MYSQL_PASSWORD` | MySQL 密码 | `root` |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `SERVER_PORT` | HTTP 服务端口 | `8080` |
| `ECHOIM_IM_PORT` | WebSocket 端口 | `8091` |
| `ECHOIM_JWT_SECRET` | JWT 签名密钥 | 需自定义 |
| `SPRING_MAIL_HOST` | 邮件 SMTP 地址 | - |
| `SPRING_MAIL_PORT` | 邮件端口 | `587` |
| `SPRING_MAIL_USERNAME` | 邮件账号 | - |
| `SPRING_MAIL_PASSWORD` | 邮件密码 | - |
| `ECHOIM_FILE_STORAGE_TYPE` | 文件存储类型 | `oss` |
| `ALIYUN_OSS_BUCKET` | OSS Bucket 名称 | - |
| `ALIYUN_OSS_ACCESS_KEY_ID` | OSS AccessKey ID | - |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | OSS AccessKey Secret | - |
| `ECHOIM_OSS_ENDPOINT` | OSS Endpoint | - |

## 文档

项目详细文档位于 `docs/` 目录：

- [系统架构](docs/architecture/) — 技术架构、分层设计、数据流
- [接口文档](docs/api/) — REST API 与 WebSocket 协议
- [数据库设计](docs/database/) — 表结构、索引策略、Redis 使用
- [部署指南](docs/deployment/) — 本地开发、Docker、Nginx、生产部署
- [产品需求](docs/prd/) — 功能清单、非功能需求
- [用户指南](docs/user-manual/) — 功能使用说明
- [UI/UX 设计](docs/design/) — 设计系统、布局、交互设计
- [未来优化](docs/roadmap/) — 工程化、性能、功能、安全、运维优化方向

## 许可证

本项目仅供学习与交流使用。
