CREATE TABLE IF NOT EXISTS im_message_reaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息反应ID',
  message_id BIGINT NOT NULL COMMENT '消息ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  emoji VARCHAR(32) NOT NULL COMMENT '反应表情',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_message_user (message_id, user_id),
  KEY idx_message_emoji (message_id, emoji),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息反应表';
