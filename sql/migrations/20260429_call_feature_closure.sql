CREATE TABLE IF NOT EXISTS `im_call_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `conversation_id` BIGINT NOT NULL,
  `call_type` VARCHAR(16) NOT NULL,
  `caller_user_id` BIGINT NOT NULL,
  `callee_user_id` BIGINT NOT NULL,
  `status` VARCHAR(24) NOT NULL,
  `started_at` DATETIME NOT NULL,
  `answered_at` DATETIME NULL,
  `ended_at` DATETIME NULL,
  `end_reason` VARCHAR(32) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_call_conversation` (`conversation_id`),
  KEY `idx_call_caller_status` (`caller_user_id`, `status`),
  KEY `idx_call_callee_status` (`callee_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
