# echoim-admin

EchoIM 管理后台前端，基于 Vue 3 + TypeScript + Element Plus 构建。仅面向系统管理员，不对普通用户开放。

## 技术栈

- **框架**：Vue 3 (Composition API) + TypeScript
- **状态管理**：Pinia
- **UI 组件**：Element Plus
- **构建工具**：Vite

## 项目结构

```
src/
├── api/            # API 请求封装
│   ├── dashboard.ts        # 数据看板 API
│   ├── reports.ts          # 举报管理 API
│   ├── sensitive-words.ts  # 敏感词管理 API
│   ├── notices.ts          # 系统公告 API
│   ├── bans.ts             # 用户封禁 API
│   └── operation-logs.ts   # 操作日志 API
├── layouts/        # 布局组件
├── router/         # 路由配置
├── stores/         # Pinia 状态管理
├── styles/         # 全局样式
├── types/          # TypeScript 类型定义
└── views/
    ├── Login.vue           # 管理员登录
    ├── dashboard/          # 数据看板
    ├── user/               # 用户管理
    ├── group/              # 群组管理
    ├── config/             # 系统配置
    ├── version/            # 版本管理
    ├── beauty-no/          # 靓号管理
    ├── report/             # 举报管理
    ├── sensitive-word/     # 敏感词管理
    ├── notice/             # 系统公告
    ├── ban/                # 用户封禁
    └── operation-log/      # 操作日志
```

## 功能模块

### 管理员登录
- 独立登录入口，与用户端隔离
- JWT 令牌认证

### 数据看板
- 核心指标卡片（总用户、今日新增、总消息、在线用户）
- 消息趋势柱状图（7 天 / 30 天切换）
- 消息类型分布条形图
- 实时在线统计面板

### 用户管理
- 用户列表查询与分页
- 用户状态管理（启用 / 禁用）
- 强制下线

### 群组管理
- 群组列表查询
- 群组解散

### 系统配置
- 系统参数在线配置
- 功能开关管理

### 版本管理
- 客户端版本发布管理
- 版本号与更新说明维护

### 靓号管理
- 靓号池管理
- 靓号分配与回收

### 举报管理
- 举报列表（按状态筛选：待处理 / 已忽略 / 已警告 / 已禁言 / 已封号）
- 审核处理操作

### 敏感词管理
- 敏感词列表查询
- 添加 / 删除敏感词
- 缓存刷新

### 系统公告
- 发布公告（全员 / 指定用户）
- 公告列表与撤回

### 用户封禁
- 封禁用户（临时 1 小时 / 1 天 / 7 天，或永久）
- 封禁历史记录
- 解除封禁

### 操作日志
- 操作日志查询（按模块名 / 操作人筛选）
- 分页浏览

## 本地开发

前置条件：
- Node.js 18+
- 后端服务已启动

启动：

```bash
npm install
npm run dev
```

管理后台默认运行在 `http://localhost:5174`，API 请求代理到后端 `/api/admin/` 路径。

## 生产构建

```bash
npm run build
```

构建产物输出到 `dist/` 目录，使用 Nginx 托管。建议通过独立路径（如 `/admin/`）或独立域名部署，与用户端隔离。
