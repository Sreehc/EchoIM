# echoim-web

EchoIM Web 聊天客户端，当前已经切到真实后端联调模式：

- 登录：`POST /api/auth/login`
- 会话列表：`GET /api/conversations`
- 历史消息：`GET /api/conversations/:id/messages`
- 已读 / 置顶 / 免打扰：`PUT /read`、`PUT /top`、`PUT /mute`
- 实时链路：`GET /api/im/info` + `WebSocket /ws`
- 重连恢复：`POST /api/offline-sync/messages`

## 本地联调

前置条件：

- MySQL 已启动，并存在 `echoim` 数据库与演示账号种子数据
- Redis 已启动
- `ECHOIM_JWT_SECRET` 必须满足 HMAC 长度要求，建议至少 32 字节
- 如果本地没有 OSS，后端需设置 `ECHOIM_FILE_STORAGE_TYPE=local`

1. 启动后端 `backend/echoim-server`
2. 确认后端 `.env` / `application.yml` 里的数据库、Redis、JWT 配置可用
3. 启动前端

```bash
npm install
npm run dev
```

默认开发代理：

- `/api -> http://127.0.0.1:8080`
- `/ws -> ws://127.0.0.1:8091`

如果你的部署不是同源代理，可以在前端目录新增 `.env.local`：

```bash
VITE_API_BASE_URL=http://127.0.0.1:8080
VITE_WS_URL=ws://127.0.0.1:8091/ws
```

后端本地联调可参考：

```bash
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DB=echoim
MYSQL_USERNAME=root
MYSQL_PASSWORD=root
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
ECHOIM_JWT_SECRET=dev-echoim-secret-key-at-least-32-bytes-long
ECHOIM_FILE_STORAGE_TYPE=local
SERVER_PORT=8080
ECHOIM_IM_PORT=8091
```

## 校验命令

```bash
npm run typecheck
npm run build
npm run test:e2e
npm run test:a11y
```

`test:e2e` 使用 Playwright 直接跑真实后端与双端实时链路，包含：

- 浏览器端单聊发送、已读、断线重连与离线补回
- 原生双 WS `DELIVERED / READ` 烟测

`test:a11y` 使用 `@axe-core/playwright` 覆盖：

- 登录页
- 聊天主界面

Playwright 当前直接走本机已安装的 `Google Chrome` 通道，不依赖额外下载浏览器。

## 当前说明

- 资料抽屉仍保留部分本地展示信息，用来补足后端暂未提供的资料详情接口
- 会话列表当前已经返回 `isMute / peerUserId / groupId`，前端发送和免打扰状态直接以真实后端为准
- 聊天页已经加入测试专用 debug hook，仅在 `DEV` 或 `VITE_ENABLE_E2E_HOOKS=true` 时暴露，不进入生产

## 构建基线

当前 `npm run build` 产物基线：

- `dist/assets/element-plus-CetHfjJ8.js` `260.03 kB` / gzip `92.24 kB`
- `dist/assets/vue-vendor-YfqYolD0.js` `30.77 kB` / gzip `12.06 kB`
- `dist/assets/ChatHomeView-BGTlJHw5.js` `29.02 kB` / gzip `8.92 kB`
- `dist/assets/index-BiPmBM1z.js` `25.54 kB` / gzip `9.04 kB`
- `dist/assets/element-plus-DuXHLxyj.css` `62.79 kB` / gzip `8.68 kB`
