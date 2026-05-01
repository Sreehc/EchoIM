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
├── layouts/        # 布局组件
├── router/         # 路由配置
├── stores/         # Pinia 状态管理
├── styles/         # 全局样式
├── types/          # TypeScript 类型定义
└── views/
    ├── Login.vue           # 管理员登录
    ├── user/               # 用户管理
    ├── group/              # 群组管理
    ├── config/             # 系统配置
    ├── version/            # 版本管理
    └── beauty-no/          # 靓号管理
```

## 功能模块

### 管理员登录
- 独立登录入口，与用户端隔离
- JWT 令牌认证

### 用户管理
- 用户列表查询与分页
- 用户状态管理（启用 / 封禁）
- 用户资料查看与编辑
- 靓号分配

### 群组管理
- 群组列表查询
- 群组详情查看
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
