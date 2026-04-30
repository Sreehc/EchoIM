ALTER TABLE im_user
  ADD UNIQUE KEY uk_email (email);

CREATE TABLE IF NOT EXISTS im_auth_trusted_device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '受信设备ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  device_name VARCHAR(120) NOT NULL COMMENT '设备名称',
  device_fingerprint VARCHAR(96) NOT NULL COMMENT '设备指纹',
  grant_token_hash VARCHAR(128) NOT NULL COMMENT '授权令牌哈希',
  expire_at DATETIME NOT NULL COMMENT '授权过期时间',
  last_used_at DATETIME DEFAULT NULL COMMENT '最近使用时间',
  revoked_at DATETIME DEFAULT NULL COMMENT '撤销时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_user_id (user_id),
  KEY idx_user_expire (user_id, expire_at),
  KEY idx_fingerprint (device_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='受信设备表';

CREATE TABLE IF NOT EXISTS im_auth_security_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '安全事件ID',
  user_id BIGINT DEFAULT NULL COMMENT '用户ID',
  event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
  event_status VARCHAR(32) NOT NULL COMMENT '事件状态',
  ip VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  user_agent VARCHAR(255) DEFAULT NULL COMMENT 'User Agent',
  detail_json TEXT DEFAULT NULL COMMENT '事件详情',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_user_created (user_id, created_at),
  KEY idx_type_created (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='安全事件表';
