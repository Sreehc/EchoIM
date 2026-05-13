# EchoIM Beta 发布检查单

## 1. 配置核验

- `MYSQL_*`、`REDIS_*`、`ECHOIM_JWT_SECRET` 已配置并可连通
- 邮件服务 `SPRING_MAIL_*` 已配置，验证码发送可用
- 文件存储已确认使用 `local` 或 `oss`，对应密钥完整
- 如需部署期引导管理员，已配置 `ECHOIM_ADMIN_SUPER_USERNAME`、`ECHOIM_ADMIN_SUPER_PASSWORD`

## 2. 管理后台核验

- 管理员账号可登录 `/api/admin/auth/login`
- 非管理员 token 访问 `/api/admin/*` 被拒绝
- 管理员登录、失败、退出能记录到操作日志
- 系统公告可以发布与撤回

## 3. 用户端核验

- 普通用户登录后能正常进入聊天首页
- 用户端可打开系统公告中心并查看历史公告
- 新公告发布后，在线用户能收到顶部轻提示
- 公告标记已读后未读数即时减少，刷新页面后状态保持一致

## 4. 回归检查

- 后端：`mvn -B test`
- 用户端：`npm run typecheck`、`npm run build`
- 管理后台：`npm run typecheck`、`npm run build`
- 如本地环境允许，补跑公告链路和核心聊天链路的 E2E 冒烟

## 5. 发布记录

- 发布版本号：
- 发布日期：
- 发布人：
- 回归结论：
- 已知问题：
