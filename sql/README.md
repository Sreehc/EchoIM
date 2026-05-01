# EchoIM 数据库脚本

## 文件说明

| 文件 | 说明 |
|------|------|
| `init.sql` | 建库建表 DDL，包含所有表结构、索引、约束 |
| `initdata.sql` | 初始数据，包含管理员账号、演示用户、示例会话与消息 |

## 执行顺序

先执行 `init.sql` 建表，再执行 `initdata.sql` 导入初始数据：

```bash
mysql -u root -p < sql/init.sql
mysql -u root -p echoim < sql/initdata.sql
```

## Docker 环境执行

使用项目根目录的 `docker-compose.yml` 启动 MySQL 后：

```bash
# 建表
docker exec -i echoim-mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4' < sql/init.sql

# 导入初始数据
docker exec -i echoim-mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4' < sql/initdata.sql
```

## 初始账号

### 管理员

| 用户名 | 密码 | 说明 |
|--------|------|------|
| `admin` | `EchoIM@Admin2026!` | 系统管理员，仅限管理后台使用 |

### 演示用户

初始数据包含 6 个演示用户（密码均为 `123456`），用于开发和测试：

| 用户名 | 昵称 |
|--------|------|
| `alice` | Alice |
| `bob` | Bob |
| `charlie` | Charlie |
| `david` | David |
| `eve` | Eve |
| `frank` | Frank |

## ID 生成规则

系统使用统一的 ID 生成策略：

| 实体 | 格式 | 示例 |
|------|------|------|
| 用户编号 (user_no) | `U` + 10 位随机数字 | `U3829104756` |
| 群组编号 (group_no) | `G` + 10 位随机数字 | `G5418203967` |
| 频道编号 (channel_no) | `CH` + 10 位随机数字 | `CH7293846150` |
| 会话编号 (conversation_no) | UUID v4 | `fb92ac3b-6f29-4413-89f9-3fa793d7922f` |

## 主要表结构

| 表名 | 说明 |
|------|------|
| `im_user` | 用户账号 |
| `im_conversation` | 会话 |
| `im_conversation_user` | 会话成员关系 |
| `im_message` | 消息 |
| `im_message_read` | 消息已读状态 |
| `im_friend` | 好友关系 |
| `im_friend_request` | 好友申请 |
| `im_group` | 群组 / 频道 |
| `im_group_member` | 群组成员 |
| `im_file` | 文件资源 |
| `im_sticker` | 贴纸 |
| `im_security_event` | 安全事件日志 |
| `im_system_config` | 系统配置 |
| `im_beauty_no` | 靓号池 |
| `im_trusted_device` | 可信设备 |

## 验证命令

```bash
# 查看数据库
mysql -u root -p -e "SHOW DATABASES LIKE 'echoim';"

# 查看所有表
mysql -u root -p echoim -e "SHOW TABLES;"

# 查看用户数据
mysql -u root -p echoim -e "SELECT id, user_no, username, nickname FROM im_user;"
```
