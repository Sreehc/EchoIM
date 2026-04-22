-- EchoIM 数据库设计脚本
-- 说明：
-- 1. 基于 MySQL 8.x
-- 2. 字符集统一为 utf8mb4
-- 3. 存储引擎统一为 InnoDB
-- 4. 生产环境建议将自增主键替换为雪花 ID 或号段 ID

CREATE DATABASE IF NOT EXISTS echoim DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE echoim;

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

-- Redis key 规划建议
-- echoim:online:user:{userId}
-- echoim:route:user:{userId}
-- echoim:heartbeat:user:{userId}
-- echoim:conversation:unread:{userId}
-- echoim:config:{configKey}
