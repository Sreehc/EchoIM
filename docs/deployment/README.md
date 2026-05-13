# EchoIM 部署指南

## 1. 环境要求

| 组件 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 7.0+ |
| Maven | 3.8+ |
| Nginx | 1.20+（生产环境） |

## 2. 本地开发启动

### 2.1 初始化数据库

```bash
mysql -u root -p < sql/init.sql
mysql -u root -p echoim < sql/initdata.sql
```

初始管理员：`admin` / `EchoIM@Admin2026!`
演示用户：`alice`~`frank` / `123456`

### 2.2 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env`，必须填写：
- `ECHOIM_JWT_SECRET`：JWT 签名密钥
- `MYSQL_*`：数据库连接
- `REDIS_*`：Redis 连接
- `ECHOIM_ADMIN_SUPER_USERNAME` / `ECHOIM_ADMIN_SUPER_PASSWORD`：可选的部署期超级管理员引导账号
- `SPRING_MAIL_*`：邮件服务（注册/登录验证码）
- `ALIYUN_OSS_*` 或使用本地存储（设置 `ECHOIM_FILE_STORAGE_TYPE=local`）

### 2.3 启动后端

```bash
cd backend/echoim-server
mvn spring-boot:run
```

- HTTP API：`http://localhost:8080`
- WebSocket：`ws://localhost:8091`

### 2.4 启动前端

```bash
# 用户端
cd frontend/echoim-web
npm install
npm run dev    # http://localhost:5173

# 管理后台
cd frontend/echoim-admin
npm install
npm run dev    # http://localhost:5174
```

Vite 开发代理已配置：
- echoim-web：`/api` → `:8080`，`/ws` → `:8091`
- echoim-admin：`/admin` → `:8080`

## 3. Docker 部署

### 3.1 一键启动基础设施

```bash
cp .env.example .env
# 编辑 .env 填写配置
docker compose up -d
```

启动 MySQL 8.4 + Redis 7.4 + echoim-server。

### 3.2 初始化数据库

```bash
docker exec -i echoim-mysql sh -lc \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4' < sql/init.sql

docker exec -i echoim-mysql sh -lc \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4' < sql/initdata.sql
```

### 3.3 构建前端

```bash
cd frontend/echoim-web && npm run build
cd frontend/echoim-admin && npm run build
```

## 4. 生产环境部署

### 4.1 Nginx 配置

```nginx
server {
    listen 80;
    server_name im.example.com;

    # 用户端静态文件
    root /srv/echoim/frontend/echoim-web/dist;
    index index.html;

    # 管理后台（独立路径或子域名）
    location /admin/ {
        alias /srv/echoim/frontend/echoim-admin/dist/;
        try_files $uri $uri/ /admin/index.html;
    }

    # REST API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket 代理
    location /ws {
        proxy_pass http://127.0.0.1:8091;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 前端路由 fallback
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### 4.2 HTTPS 配置（推荐）

```nginx
server {
    listen 443 ssl http2;
    server_name im.example.com;

    ssl_certificate /etc/letsencrypt/live/im.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/im.example.com/privkey.pem;

    # ... 同上 location 配置
}

server {
    listen 80;
    server_name im.example.com;
    return 301 https://$host$request_uri;
}
```

WebSocket 生产环境使用 `wss://` 协议。

## 5. 环境变量参考

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MYSQL_HOST` | MySQL 地址 | `localhost` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_DB` | 数据库名 | `echoim` |
| `MYSQL_USERNAME` | MySQL 用户名 | `root` |
| `MYSQL_PASSWORD` | MySQL 密码 | `root` |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | 空 |
| `SERVER_PORT` | HTTP 端口 | `8080` |
| `ECHOIM_IM_PORT` | WebSocket 端口 | `8091` |
| `ECHOIM_JWT_SECRET` | JWT 密钥 | **必填** |
| `SPRING_MAIL_HOST` | SMTP 地址 | **必填** |
| `SPRING_MAIL_PORT` | SMTP 端口 | `587` |
| `SPRING_MAIL_USERNAME` | SMTP 账号 | **必填** |
| `SPRING_MAIL_PASSWORD` | SMTP 密码 | **必填** |
| `SPRING_MAIL_FROM` | 发件人 | `EchoIM <mailer@example.com>` |
| `ECHOIM_ADMIN_SUPER_USERNAME` | 部署期超级管理员账号 | 空 |
| `ECHOIM_ADMIN_SUPER_PASSWORD` | 部署期超级管理员密码 | 空 |
| `ECHOIM_ADMIN_SUPER_NICKNAME` | 部署期超级管理员昵称 | `系统管理员` |
| `ECHOIM_FILE_STORAGE_TYPE` | 存储类型 | `oss` |
| `ECHOIM_FILE_MAX_SIZE` | 最大文件大小 | `104857600`（100MB） |
| `ALIYUN_OSS_BUCKET` | OSS Bucket | - |
| `ALIYUN_OSS_ACCESS_KEY_ID` | OSS AK | - |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | OSS SK | - |
| `ECHOIM_OSS_ENDPOINT` | OSS Endpoint | - |

## 6. 健康检查

```bash
# HTTP 健康检查
curl http://localhost:8080/api/health

# WebSocket 连接测试
wscat -c ws://localhost:8091/ws

# 查看日志
docker compose logs -f echoim-server
```

## 7. 升级与回滚

```bash
# 拉取最新代码
git pull

# 重新构建
docker compose build echoim-server
cd frontend/echoim-web && npm run build
cd frontend/echoim-admin && npm run build

# 重启服务
docker compose up -d echoim-server

# 回滚：切回上一提交后重复构建
git checkout <previous-commit>
```

## 8. TURN 服务器（视频通话）

视频通话需要 TURN 服务器进行 NAT 穿透。在 `.env` 中配置：

```env
ECHOIM_CALL_ICE_SERVERS=[{"urls":["stun:stun.l.google.com:19302"]},{"urls":["turn:your-turn-server:3478"],"username":"user","credential":"pass"}]
```

推荐使用 [coturn](https://github.com/coturn/coturn) 自建或接入 Twilio 等第三方服务。

## 9. Beta 发布检查单

Beta 发布前请至少完成以下检查：

- 超级管理员账号可正常登录，普通用户 token 不能访问任意 `/api/admin/*` 接口
- 用户端登录后能拉取系统公告，在线用户能收到新公告提示
- 系统公告可进入详情并标记已读，刷新后未读数状态保持一致
- 后端 `mvn test`、用户端与管理后台 `typecheck` 和 `build` 全部通过
- `.env` 中 JWT、数据库、Redis、邮件、文件存储和管理员引导账号配置已核验
- 已记录本次发布版本、回归结果和已知问题
