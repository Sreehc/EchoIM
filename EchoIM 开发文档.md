# EchoIM 开发文档

## 1. 文档说明

本文档基于此前项目规划整理为统一开发文档，并补充实现级设计内容。

当前文档结构如下：

- 总览文档：[EchoIM 开发文档.md](/Users/cheers/Desktop/workspace/EchoIM/EchoIM%20开发文档.md)
- 接口文档：[接口文档.md](/Users/cheers/Desktop/workspace/EchoIM/接口文档.md)
- 数据库脚本：[数据库设计.sql](/Users/cheers/Desktop/workspace/EchoIM/sql/数据库设计.sql)
- 初始化数据脚本：[初始化数据.sql](/Users/cheers/Desktop/workspace/EchoIM/sql/初始化数据.sql)

文档目标不是继续停留在“功能清单”，而是形成一套可以直接指导前端、后端、数据库和联调开发的文档集合。

---

## 2. 项目概述

### 2.1 项目名称

EchoIM

### 2.2 项目定位

EchoIM 是一个基于前后端分离架构的网页版即时通讯系统，面向以下场景：

- 用户注册、登录与身份认证
- 好友关系建立与联系人管理
- 单聊与群聊实时通信
- 图片、视频、文件等多类型消息
- 离线消息补发与消息状态同步
- 平台后台治理与基础运营管理

### 2.3 建设目标

EchoIM 需要实现三类核心能力：

1. 面向普通用户的聊天客户端
2. 面向平台运营的管理后台
3. 面向消息投递与业务支撑的后端服务体系

最终目标是交付一个具备基础可用性、实时通信能力、可扩展性和工程实践价值的 Web IM 项目。

---

## 3. 项目范围

### 3.1 客户端范围

- 注册
- 登录
- 退出登录
- 用户资料维护
- 搜索用户
- 添加好友
- 好友申请处理
- 联系人列表
- 新朋友列表
- 会话列表
- 单聊
- 群聊
- 图片消息
- 视频消息
- 文件消息
- 历史消息加载
- 未读消息数
- 消息提醒
- 版本信息展示

### 3.2 管理后台范围

- 管理员登录
- 用户管理
- 群组管理
- 靓号管理
- 系统设置
- 版本管理
- 操作日志

### 3.3 服务端范围

- 用户认证与鉴权
- 联系人与群组关系维护
- HTTP 接口服务
- WebSocket/Netty 实时消息服务
- 文件上传下载
- 离线消息补发
- 消息确认与状态同步
- Redis 在线状态与缓存
- 集群消息分发

---

## 4. 总体架构设计

### 4.1 架构分层

EchoIM 采用前后端分离架构，整体分为五层：

1. 表现层  
   聊天客户端和管理后台，负责页面展示、用户交互、状态管理和实时消息接收。

2. 接口层  
   基于 Spring Boot 提供 REST API，负责登录、联系人、群组、会话、文件、后台管理等业务接口。

3. 实时通信层  
   基于 Netty 或 Spring WebSocket 实现长连接服务，负责消息实时推送、在线状态、心跳、确认和补发。

4. 数据层  
   使用 MySQL 存储用户、好友、群组、会话、消息、配置和版本信息等持久化数据。

5. 缓存与分发层  
   使用 Redis 存储在线状态、热点会话、临时未读计数和集群消息路由信息，通过 Pub/Sub 或 Redisson 支撑多实例消息分发。

### 4.2 部署组成

当前阶段建议采用单体后端方案，按以下模块组织：

- `echoim-web`：聊天客户端前端
- `echoim-admin`：管理后台前端
- `echoim-server`：主后端工程，内部同时承载业务 API 与 IM 实时模块
- `mysql`：业务数据库
- `redis`：缓存与消息分发
- `nginx`：静态资源与反向代理

其中 `echoim-server` 内部建议继续按包结构拆分：

- `auth`：认证与登录
- `user`：用户资料
- `friend`：好友与申请
- `group`：群组与群成员
- `conversation`：会话与未读
- `message`：消息业务
- `file`：文件上传下载
- `admin`：后台管理
- `im`：WebSocket/Netty、心跳、在线状态、ACK、离线补发

### 4.3 核心调用链路

1. 用户通过 HTTP 完成注册登录并获取 Token。
2. 客户端使用 Token 建立 WebSocket 连接。
3. 用户在聊天窗口发送消息。
4. 服务端校验发送权限并落库消息。
5. `echoim-server` 内部的 IM 模块将消息推送给在线接收方。
6. 接收方返回 ack 或 read 回执。
7. 若接收方离线，则消息在下次上线时进行补发。
8. 会话列表、未读数、最新消息摘要同步更新。

---

## 5. 技术栈设计

### 5.1 前端技术栈

#### 聊天客户端

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus
- WebSocket
- Sass

#### 管理后台

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus
- ECharts

### 5.2 后端技术栈

- Java 17
- Spring Boot 3.x
- Spring MVC
- Spring Security 或 Sa-Token/JWT
- Hibernate Validator
- MyBatis-Plus 或 MyBatis
- Netty
- Redis
- Redisson
- MySQL 8.x
- MinIO 或本地文件存储
- Maven

### 5.3 工程与运维技术栈

- Docker
- Docker Compose
- Nginx
- Knife4j 或 Swagger OpenAPI
- JUnit 5
- Spring Boot Test
- Postman/Apifox

### 5.4 技术栈选型说明

1. Vue 3 + Pinia 适合前后端分离项目，状态组织清晰。
2. Element Plus 足以覆盖聊天客户端和后台管理的大部分基础组件。
3. Spring Boot 负责业务接口，Netty 负责高频实时通信，两者职责明确。
4. Redis 不只用于缓存，也用于在线状态与集群消息分发。
5. 文件存储建议抽象成统一资源服务，方便后续从本地切到 MinIO 或 OSS。

---

## 6. 前端开发设计

### 6.1 聊天客户端页面规划

#### 认证相关页面

- 登录页
- 注册页
- 找回密码页（可选）

#### 主业务页面

- 首页布局页
- 会话列表页
- 联系人列表页
- 新朋友页
- 联系人详情页
- 单聊页
- 群聊页
- 个人设置页
- 版本说明页

### 6.2 聊天客户端功能拆解

#### 1. 账号模块

- 用户注册
- 用户登录
- Token 持久化
- 路由守卫
- 退出登录
- 个人资料查看与编辑
- 修改密码

#### 2. 联系人模块

- 搜索用户
- 发起好友申请
- 查看申请列表
- 同意申请
- 拒绝申请
- 拉黑用户
- 删除好友
- 联系人详情展示

#### 3. 群组模块

- 新建群聊
- 查看群详情
- 群成员列表
- 添加群成员
- 移除群成员
- 退出群聊
- 解散群聊

#### 4. 会话模块

- 会话列表拉取
- 会话搜索
- 会话置顶
- 会话删除
- 未读数展示
- 最新消息摘要展示
- 从会话进入聊天窗口

#### 5. 聊天模块

- 文字消息
- 表情消息
- 图片消息
- 视频消息
- 文件消息
- 消息发送状态展示
- 历史消息分页加载
- 图片预览
- 视频预览
- 文件下载

#### 6. 消息提醒模块

- 新消息提醒
- 好友申请提醒
- 会话实时刷新
- 未读数刷新
- 在线状态展示

### 6.3 管理后台页面规划

- 管理员登录页
- 用户管理页
- 群组管理页
- 系统设置页
- 版本管理页
- 靓号管理页
- 操作日志页

### 6.4 管理后台功能拆解

#### 用户管理

- 用户分页列表
- 按账号、昵称、状态搜索
- 禁用用户
- 启用用户
- 强制用户下线

#### 群组管理

- 群组分页列表
- 查看群信息
- 查看群成员
- 解散群组
- 异常群治理

#### 系统设置

- 新增配置
- 修改配置
- 查询配置
- 热点配置缓存刷新

#### 版本管理

- 新增版本
- 修改版本
- 发布版本
- 灰度比例设置
- 更新说明维护

#### 靓号管理

- 新增靓号
- 删除靓号
- 绑定用户
- 靓号状态维护

#### 操作日志

- 查看管理员操作日志
- 按模块和时间筛选

### 6.5 前端状态管理建议

Pinia 建议拆分为以下 store：

- `useAuthStore`：登录态、Token、当前用户
- `useContactStore`：联系人、好友申请、黑名单
- `useConversationStore`：会话列表、当前会话、未读数
- `useMessageStore`：消息列表、发送状态、历史消息分页
- `useWsStore`：连接状态、心跳、消息订阅
- `useSettingStore`：个人设置、版本信息、系统开关

### 6.6 前端目录建议

```text
src/
  api/
  assets/
  components/
  constants/
  layout/
  router/
  stores/
  utils/
  views/
    auth/
    contact/
    conversation/
    chat/
    setting/
    admin/
```

---

## 7. 后端开发设计

### 7.1 后端模块划分

建议将后端按业务拆分为以下模块：

- `auth`：注册、登录、Token 鉴权、管理员登录
- `user`：用户资料、用户搜索、状态管理
- `friend`：好友申请、好友关系、拉黑与删除
- `group`：群组管理、群成员管理
- `conversation`：会话列表、未读数、置顶、删除
- `message`：消息落库、历史消息、消息状态
- `file`：文件上传、下载、资源元数据
- `im`：WebSocket/Netty 连接、消息推送、心跳、补发
- `admin`：后台管理、系统配置、版本、靓号、日志

### 7.2 后端分层建议

- `controller`：HTTP 接口层
- `service`：业务编排层
- `manager`：复杂领域逻辑与跨模块聚合
- `mapper`：数据库访问层
- `domain/entity`：实体对象
- `dto/vo`：请求与响应模型
- `ws/handler`：实时消息处理器

### 7.3 认证与鉴权设计

#### 用户端

- 登录后签发 JWT Token
- Token 中包含 `userId`、`username`、`tokenType`
- HTTP 请求走拦截器鉴权
- WebSocket 握手阶段校验 Token

#### 后台端

- 独立管理员账号体系
- 角色建议至少分为 `super_admin` 和 `operator`
- 所有后台操作写入操作日志

### 7.4 HTTP 接口分组

#### 认证接口

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `POST /api/auth/change-password`

#### 用户接口

- `GET /api/users/me`
- `PUT /api/users/me`
- `GET /api/users/search`

#### 联系人接口

- `GET /api/friends`
- `POST /api/friend-requests`
- `GET /api/friend-requests`
- `PUT /api/friend-requests/{id}/approve`
- `PUT /api/friend-requests/{id}/reject`
- `DELETE /api/friends/{friendId}`

#### 群组接口

- `POST /api/groups`
- `GET /api/groups/{groupId}`
- `POST /api/groups/{groupId}/members`
- `DELETE /api/groups/{groupId}/members/{userId}`
- `DELETE /api/groups/{groupId}`

#### 会话接口

- `GET /api/conversations`
- `GET /api/conversations/{id}/messages`
- `PUT /api/conversations/{id}/top`
- `DELETE /api/conversations/{id}`
- `PUT /api/conversations/{id}/read`

#### 文件接口

- `POST /api/files/upload`
- `GET /api/files/{id}`
- `GET /api/files/{id}/download`

#### 后台接口

- `POST /admin/auth/login`
- `GET /admin/users`
- `PUT /admin/users/{id}/status`
- `PUT /admin/users/{id}/offline`
- `GET /admin/groups`
- `DELETE /admin/groups/{id}`
- `GET /admin/configs`
- `POST /admin/configs`
- `PUT /admin/configs/{id}`
- `GET /admin/versions`
- `POST /admin/versions`
- `PUT /admin/versions/{id}`
- `GET /admin/beauty-nos`
- `POST /admin/beauty-nos`
- `DELETE /admin/beauty-nos/{id}`

### 7.5 HTTP 响应规范

建议统一返回格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "202604221230001234"
}
```

分页接口建议统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "pageNo": 1,
    "pageSize": 20,
    "total": 0
  }
}
```

### 7.6 WebSocket/Netty 协议设计

#### 消息类型

- `AUTH`：连接认证
- `PING`：心跳请求
- `PONG`：心跳响应
- `CHAT_SINGLE`：单聊消息
- `CHAT_GROUP`：群聊消息
- `ACK`：送达确认
- `READ`：已读确认
- `NOTICE`：系统通知
- `FORCE_OFFLINE`：强制下线
- `OFFLINE_SYNC`：离线消息同步

#### WebSocket 报文建议

```json
{
  "type": "CHAT_SINGLE",
  "traceId": "trace-001",
  "clientMsgId": "cmsg-123456",
  "timestamp": 1713772800000,
  "data": {
    "conversationId": 1001,
    "toUserId": 2002,
    "msgType": "TEXT",
    "content": "hello"
  }
}
```

#### 服务端回包建议

```json
{
  "type": "ACK",
  "traceId": "trace-001",
  "clientMsgId": "cmsg-123456",
  "timestamp": 1713772800020,
  "data": {
    "serverMsgId": 90000001,
    "conversationId": 1001,
    "status": "DELIVERED"
  }
}
```

### 7.7 消息处理核心流程

#### 单聊消息流程

1. 客户端生成 `clientMsgId` 并发送消息。
2. 服务端校验 Token、好友关系和发送权限。
3. 服务端创建或查找单聊会话。
4. 服务端为会话分配递增 `seqNo`。
5. 消息持久化到 `im_message`。
6. 若接收方在线，立即推送消息。
7. 更新发送方和接收方会话摘要、未读数。
8. 返回 ACK 给发送方。
9. 接收方已读时，发送 READ 回执。

#### 群聊消息流程

1. 客户端发送群消息。
2. 服务端校验群成员身份和群状态。
3. 持久化消息。
4. 广播给在线群成员。
5. 批量更新群成员会话未读数。
6. 记录送达或已读回执。

### 7.8 离线消息与重连策略

- 客户端重连后立即触发离线同步
- 以 `lastReadSeq` 或最后成功拉取时间作为补偿游标
- 服务端返回游标之后的新消息
- ACK 已送达，READ 代表已读，两者区分处理
- 对重复 `clientMsgId` 做幂等去重

### 7.9 文件消息策略

- 先上传文件，拿到 `fileId`
- 再发送文件类型消息
- 消息体中只存资源引用，不直接存二进制数据
- 图片、视频和普通文件统一走 `im_file`
- 上传大小、类型、后缀、内容类型都需要校验

### 7.10 集群扩展策略

- Redis 记录用户当前连接所在实例
- 发送消息时先查路由
- 同实例直接推送
- 跨实例通过 Redis Pub/Sub 或 Redisson topic 分发
- 连接断开时清理在线状态与路由缓存

---

## 8. 数据库设计

### 8.1 设计原则

1. 核心业务数据落 MySQL，缓存与临时态落 Redis。
2. 单聊和群聊消息尽量复用统一消息表，避免两套逻辑。
3. 会话与消息分离，会话负责摘要和未读，消息负责明细。
4. 文件资源独立建表，避免消息表承载过多媒体字段。
5. 后台管理使用独立管理员表和操作日志表。

### 8.2 核心表清单

- `im_user`：用户表
- `im_friend_request`：好友申请表
- `im_friend`：好友关系表
- `im_group`：群组表
- `im_group_member`：群成员表
- `im_conversation`：会话表
- `im_conversation_user`：用户会话表
- `im_file`：文件资源表
- `im_message`：消息表
- `im_message_receipt`：消息回执表
- `sys_admin_user`：管理员表
- `sys_config`：系统配置表
- `sys_version`：版本信息表
- `sys_beauty_no`：靓号表
- `sys_operation_log`：操作日志表

### 8.3 MySQL 建表 SQL

说明：

- 字符集统一使用 `utf8mb4`
- 存储引擎使用 `InnoDB`
- 生产环境建议主键使用雪花 ID 或号段 ID
- 以下 SQL 以 MySQL 8 为基准，可直接作为初版表结构

```sql
CREATE TABLE IF NOT EXISTS im_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  user_no VARCHAR(32) NOT NULL COMMENT '用户编号或登录号',
  username VARCHAR(50) NOT NULL COMMENT '用户名',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  nickname VARCHAR(50) NOT NULL COMMENT '昵称',
  avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像',
  gender TINYINT NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  signature VARCHAR(255) DEFAULT NULL COMMENT '个性签名',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 2禁用 3注销',
  last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_user_no (user_no),
  UNIQUE KEY uk_username (username),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS im_friend_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友申请ID',
  from_user_id BIGINT NOT NULL COMMENT '申请人ID',
  to_user_id BIGINT NOT NULL COMMENT '接收人ID',
  apply_msg VARCHAR(255) DEFAULT NULL COMMENT '申请留言',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1同意 2拒绝 3拉黑',
  handled_by BIGINT DEFAULT NULL COMMENT '处理人ID',
  handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_to_user_status (to_user_id, status),
  KEY idx_from_user (from_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友申请表';

CREATE TABLE IF NOT EXISTS im_friend (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友关系ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  friend_user_id BIGINT NOT NULL COMMENT '好友用户ID',
  remark VARCHAR(100) DEFAULT NULL COMMENT '好友备注',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 2拉黑 3删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_user_friend (user_id, friend_user_id),
  KEY idx_friend_user (friend_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

CREATE TABLE IF NOT EXISTS im_group (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群组ID',
  group_no VARCHAR(32) NOT NULL COMMENT '群编号',
  group_name VARCHAR(100) NOT NULL COMMENT '群名称',
  owner_user_id BIGINT NOT NULL COMMENT '群主ID',
  avatar_url VARCHAR(255) DEFAULT NULL COMMENT '群头像',
  notice TEXT DEFAULT NULL COMMENT '群公告',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 2解散 3禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_group_no (group_no),
  KEY idx_owner_user (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组表';

CREATE TABLE IF NOT EXISTS im_group_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群成员ID',
  group_id BIGINT NOT NULL COMMENT '群组ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role TINYINT NOT NULL DEFAULT 2 COMMENT '1群主 2成员 3管理员',
  nick_name VARCHAR(100) DEFAULT NULL COMMENT '群内昵称',
  join_source TINYINT NOT NULL DEFAULT 1 COMMENT '1创建群 2邀请加入 3申请加入',
  join_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 2退出 3移除',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_group_user (group_id, user_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员表';

CREATE TABLE IF NOT EXISTS im_conversation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
  conversation_type TINYINT NOT NULL COMMENT '1单聊 2群聊',
  biz_key VARCHAR(64) NOT NULL COMMENT '单聊为较小用户ID_较大用户ID，群聊为group_{groupId}',
  biz_id BIGINT DEFAULT NULL COMMENT '业务ID，群聊时为groupId',
  conversation_name VARCHAR(100) DEFAULT NULL COMMENT '会话名称',
  avatar_url VARCHAR(255) DEFAULT NULL COMMENT '会话头像',
  last_message_id BIGINT DEFAULT NULL COMMENT '最后消息ID',
  last_message_preview VARCHAR(500) DEFAULT NULL COMMENT '最后消息摘要',
  last_message_time DATETIME DEFAULT NULL COMMENT '最后消息时间',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 2删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_type_biz_key (conversation_type, biz_key),
  KEY idx_last_message_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

CREATE TABLE IF NOT EXISTS im_conversation_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户会话ID',
  conversation_id BIGINT NOT NULL COMMENT '会话ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  unread_count INT NOT NULL DEFAULT 0 COMMENT '未读数',
  last_read_seq BIGINT NOT NULL DEFAULT 0 COMMENT '最后已读序号',
  is_top TINYINT NOT NULL DEFAULT 0 COMMENT '0否 1是',
  is_mute TINYINT NOT NULL DEFAULT 0 COMMENT '0否 1是',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0否 1是',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_conversation_user (conversation_id, user_id),
  KEY idx_user_id_top (user_id, is_top)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';

CREATE TABLE IF NOT EXISTS im_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
  owner_user_id BIGINT NOT NULL COMMENT '上传用户ID',
  biz_type TINYINT NOT NULL COMMENT '1头像 2图片 3视频 4普通文件',
  storage_type VARCHAR(20) NOT NULL DEFAULT 'local' COMMENT 'local/minio/oss',
  bucket_name VARCHAR(100) DEFAULT NULL COMMENT '桶名',
  object_key VARCHAR(255) NOT NULL COMMENT '对象路径',
  file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  file_ext VARCHAR(20) DEFAULT NULL COMMENT '文件后缀',
  content_type VARCHAR(100) DEFAULT NULL COMMENT '内容类型',
  file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小字节',
  md5 VARCHAR(32) DEFAULT NULL COMMENT 'MD5',
  url VARCHAR(255) DEFAULT NULL COMMENT '访问地址',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1有效 2删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_owner_user_id (owner_user_id),
  KEY idx_biz_type (biz_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件资源表';

CREATE TABLE IF NOT EXISTS im_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
  conversation_id BIGINT NOT NULL COMMENT '会话ID',
  conversation_type TINYINT NOT NULL COMMENT '1单聊 2群聊',
  seq_no BIGINT NOT NULL COMMENT '会话内递增序号',
  client_msg_id VARCHAR(64) NOT NULL COMMENT '客户端消息ID',
  from_user_id BIGINT NOT NULL COMMENT '发送人ID',
  to_user_id BIGINT DEFAULT NULL COMMENT '接收人ID，单聊时使用',
  group_id BIGINT DEFAULT NULL COMMENT '群ID，群聊时使用',
  msg_type TINYINT NOT NULL COMMENT '1文本 2表情 3图片 4视频 5文件 6系统消息',
  content TEXT DEFAULT NULL COMMENT '消息内容',
  extra_json JSON DEFAULT NULL COMMENT '扩展数据',
  file_id BIGINT DEFAULT NULL COMMENT '文件ID',
  send_status TINYINT NOT NULL DEFAULT 1 COMMENT '1发送成功 2发送失败 3撤回',
  sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_from_client_msg (from_user_id, client_msg_id),
  UNIQUE KEY uk_conversation_seq (conversation_id, seq_no),
  KEY idx_conversation_time (conversation_id, sent_at),
  KEY idx_to_user_id (to_user_id),
  KEY idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

CREATE TABLE IF NOT EXISTS im_message_receipt (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '回执ID',
  message_id BIGINT NOT NULL COMMENT '消息ID',
  conversation_id BIGINT NOT NULL COMMENT '会话ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  receipt_type TINYINT NOT NULL COMMENT '1送达 2已读',
  receipt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回执时间',
  UNIQUE KEY uk_message_user_receipt (message_id, user_id, receipt_type),
  KEY idx_conversation_user (conversation_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息回执表';

CREATE TABLE IF NOT EXISTS sys_admin_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID',
  username VARCHAR(50) NOT NULL COMMENT '管理员账号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  nickname VARCHAR(50) NOT NULL COMMENT '管理员昵称',
  role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 2禁用',
  last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
  config_key VARCHAR(100) NOT NULL COMMENT '配置键',
  config_value TEXT NOT NULL COMMENT '配置值',
  config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS sys_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本ID',
  version_code VARCHAR(50) NOT NULL COMMENT '版本号',
  version_name VARCHAR(100) NOT NULL COMMENT '版本名称',
  platform VARCHAR(20) NOT NULL DEFAULT 'web' COMMENT '平台',
  release_note TEXT DEFAULT NULL COMMENT '更新说明',
  force_update TINYINT NOT NULL DEFAULT 0 COMMENT '0否 1是',
  gray_percent INT NOT NULL DEFAULT 100 COMMENT '灰度百分比',
  publish_status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布',
  published_at DATETIME DEFAULT NULL COMMENT '发布时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_version_code (version_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本信息表';

CREATE TABLE IF NOT EXISTS sys_beauty_no (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '靓号ID',
  beauty_no VARCHAR(32) NOT NULL COMMENT '靓号',
  bind_user_id BIGINT DEFAULT NULL COMMENT '绑定用户ID',
  level_type TINYINT NOT NULL DEFAULT 1 COMMENT '1普通 2稀有 3高价值',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1未使用 2已绑定 3停用',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_beauty_no (beauty_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='靓号表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  admin_user_id BIGINT NOT NULL COMMENT '管理员ID',
  module_name VARCHAR(50) NOT NULL COMMENT '模块名',
  action_name VARCHAR(50) NOT NULL COMMENT '操作名',
  target_type VARCHAR(50) DEFAULT NULL COMMENT '目标类型',
  target_id BIGINT DEFAULT NULL COMMENT '目标ID',
  request_ip VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
  content_json JSON DEFAULT NULL COMMENT '日志详情',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_admin_user_id (admin_user_id),
  KEY idx_module_name (module_name),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

### 8.4 Redis 设计建议

建议使用以下 key 规划：

- `echoim:online:user:{userId}`：用户在线状态
- `echoim:route:user:{userId}`：用户所在实例
- `echoim:heartbeat:user:{userId}`：最后心跳时间
- `echoim:conversation:unread:{userId}`：用户总未读数
- `echoim:config:{configKey}`：热点配置缓存

---

## 9. 关键业务规则

### 9.1 单聊会话生成规则

- 单聊会话按用户 ID 排序生成固定 `bizKey`
- 相同两个人只存在一个单聊会话
- 第一次发送消息时自动创建会话及双方用户会话记录

### 9.2 群聊会话生成规则

- 群创建成功时同步创建群聊会话
- 群成员加入时创建或恢复 `im_conversation_user`
- 群解散后会话状态改为不可用

### 9.3 未读数规则

- 接收新消息时增加接收方未读数
- 当前会话打开且消息实时到达时可直接置为已读
- 用户主动进入会话或点击已读时更新 `lastReadSeq`

### 9.4 消息幂等规则

- 每条客户端消息必须携带 `clientMsgId`
- 服务端对 `(from_user_id, client_msg_id)` 建唯一索引
- 重发时如果已存在则直接返回原消息结果

### 9.5 安全规则

- 登录接口需要限流
- 文件上传需要限制大小和类型
- 后台接口必须鉴权和角色校验
- 强制下线需要同时通知 WebSocket 连接

---

## 10. 开发阶段安排

### 第 1 期：基础架构与技术底座

- 初始化前后端工程
- 建立数据库初版表结构
- 封装统一响应、异常处理、鉴权
- 搭建前端路由和公共布局

### 第 2 期：账号体系与关系链路

- 注册登录
- 用户资料
- 好友申请与好友列表
- 建群与基础群成员管理

### 第 3 期：聊天 MVP 核心闭环

- WebSocket/Netty 建连
- 单聊文字消息
- 群聊文字消息
- 会话列表
- 未读数和消息提醒

### 第 4 期：消息增强与可用性完善

- 图片、视频、文件消息
- 离线消息补发
- 消息状态同步
- 重连恢复

### 第 5 期：后台管理与平台运营能力

- 用户管理
- 群组管理
- 系统设置
- 版本管理
- 靓号管理
- 操作日志

### 第 6 期：集群化、测试与交付

- Redis 路由与消息分发
- 多实例部署
- 性能与稳定性测试
- Docker 和 Nginx 部署
- 文档整理与项目交付

---

## 11. 测试计划

### 11.1 功能测试

- 注册登录流程
- 好友申请与处理流程
- 单聊消息收发
- 群聊广播
- 文件上传下载
- 后台用户和群组治理

### 11.2 接口测试

- HTTP 参数校验
- 鉴权测试
- 文件接口测试
- WebSocket 连接和鉴权测试
- ACK/READ 回执测试

### 11.3 场景测试

- 单用户在线聊天
- 多用户并发群聊
- 用户离线再上线补发
- 网络抖动后自动重连
- 管理员强制下线用户

### 11.4 性能测试

- 长连接稳定性
- 高并发消息推送
- Redis 命中率
- 会话列表查询性能
- 历史消息分页性能

---

## 12. 部署建议

### 12.1 开发环境

- 前端本地启动 Vite
- 后端本地启动 Spring Boot
- MySQL 与 Redis 使用 Docker

### 12.2 测试与生产环境

- Nginx 代理前端静态资源和 API
- API 服务与 IM 服务分开部署
- Redis 支持在线状态与路由缓存
- MySQL 独立部署并开启定期备份
- 文件存储建议独立到 MinIO 或对象存储

### 12.3 生产优化建议

- 接口与消息链路增加日志追踪 ID
- 建立慢 SQL 和异常告警
- 对登录、上传、群发等接口增加限流
- 对文件访问增加鉴权或签名机制

---

## 13. 最终结论

EchoIM 的开发重点不应是“把所有页面先堆出来”，而应是先完成核心消息闭环，再扩展媒体能力、后台治理和集群能力。

本开发文档已经将项目规划、优化分期、前后端功能、技术栈和数据库设计统一收敛为一份总文档，后续可以在此基础上继续细化：

- 接口详细定义
- WebSocket 协议字段表
- 前端页面原型
- Java 包结构与代码规范
- 初始化 SQL 脚本与演示数据

如果继续推进，下一步最合理的是先补一份“接口文档 + 字段字典 + 初始化数据脚本”。
