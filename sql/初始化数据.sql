-- EchoIM 初始化数据脚本
-- 说明：
-- 1. 请先执行 数据库设计.sql
-- 2. 以下数据用于本地开发、接口联调和页面演示
-- 3. 演示账号与管理员默认密码均为 123456，已写入真实 BCrypt 哈希

USE echoim;

SET FOREIGN_KEY_CHECKS = 0;

-- 管理员初始化
INSERT INTO sys_admin_user (id, username, password_hash, nickname, role_code, status, created_at, updated_at)
VALUES
  (1, 'admin', '$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y', '系统管理员', 'super_admin', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  nickname = VALUES(nickname),
  role_code = VALUES(role_code),
  status = VALUES(status),
  updated_at = NOW();

-- 系统配置初始化
INSERT INTO sys_config (id, config_key, config_value, config_name, remark, status, created_at, updated_at)
VALUES
  (1, 'file.max-size-mb', '50', '文件上传大小限制', '上传文件最大 50MB', 1, NOW(), NOW()),
  (2, 'message.recall-seconds', '120', '消息撤回时间限制', '发送后 120 秒内可撤回', 1, NOW(), NOW()),
  (3, 'register.enabled', 'true', '是否允许注册', '控制前台注册开关', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  config_name = VALUES(config_name),
  remark = VALUES(remark),
  status = VALUES(status),
  updated_at = NOW();

-- 版本信息初始化
INSERT INTO sys_version (id, version_code, version_name, platform, release_note, force_update, gray_percent, publish_status, published_at, created_at, updated_at)
VALUES
  (1, 'v0.1.0', 'EchoIM MVP', 'web', '初始演示版本，支持注册登录、联系人、单聊和群聊文字消息。', 0, 100, 1, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
  version_name = VALUES(version_name),
  release_note = VALUES(release_note),
  gray_percent = VALUES(gray_percent),
  publish_status = VALUES(publish_status),
  published_at = VALUES(published_at),
  updated_at = NOW();

-- 靓号初始化
INSERT INTO sys_beauty_no (id, beauty_no, bind_user_id, level_type, status, remark, created_at, updated_at)
VALUES
  (1, '10000', NULL, 2, 1, '演示稀有靓号', NOW(), NOW()),
  (2, '88888', NULL, 3, 1, '演示高价值靓号', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  level_type = VALUES(level_type),
  status = VALUES(status),
  remark = VALUES(remark),
  updated_at = NOW();

-- 普通用户初始化
INSERT INTO im_user (id, user_no, username, password_hash, nickname, avatar_url, gender, phone, email, signature, status, last_login_at, created_at, updated_at)
VALUES
  (10001, 'E10001', 'echo_demo_01', '$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y', 'Echo用户01', NULL, 1, '13800000001', 'echo01@example.com', '欢迎来到 EchoIM', 1, NOW(), NOW(), NOW()),
  (10002, 'E10002', 'echo_demo_02', '$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y', 'Echo用户02', NULL, 2, '13800000002', 'echo02@example.com', '在线沟通更高效', 1, NOW(), NOW(), NOW()),
  (10003, 'E10003', 'echo_demo_03', '$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y', 'Echo用户03', NULL, 1, '13800000003', 'echo03@example.com', '这是第三个演示账号', 1, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  nickname = VALUES(nickname),
  phone = VALUES(phone),
  email = VALUES(email),
  signature = VALUES(signature),
  status = VALUES(status),
  updated_at = NOW();

-- 好友申请与好友关系初始化
INSERT INTO im_friend_request (id, from_user_id, to_user_id, apply_msg, status, handled_by, handled_at, created_at, updated_at)
VALUES
  (1, 10001, 10002, '你好，想加你为好友', 1, 10002, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  handled_by = VALUES(handled_by),
  handled_at = VALUES(handled_at),
  updated_at = NOW();

INSERT INTO im_friend (id, user_id, friend_user_id, remark, status, created_at, updated_at)
VALUES
  (1, 10001, 10002, '产品同学', 1, NOW(), NOW()),
  (2, 10002, 10001, '研发同学', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  remark = VALUES(remark),
  status = VALUES(status),
  updated_at = NOW();

-- 群组与群成员初始化
INSERT INTO im_group (id, group_no, group_name, owner_user_id, avatar_url, notice, status, created_at, updated_at)
VALUES
  (20001, 'G20001', 'Echo 项目讨论群', 10001, NULL, '欢迎加入 EchoIM 项目讨论群。', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  group_name = VALUES(group_name),
  notice = VALUES(notice),
  status = VALUES(status),
  updated_at = NOW();

INSERT INTO im_group_member (id, group_id, user_id, role, nick_name, join_source, join_at, status, updated_at)
VALUES
  (1, 20001, 10001, 1, '群主-01', 1, NOW(), 1, NOW()),
  (2, 20001, 10002, 2, '成员-02', 2, NOW(), 1, NOW()),
  (3, 20001, 10003, 2, '成员-03', 2, NOW(), 1, NOW())
ON DUPLICATE KEY UPDATE
  role = VALUES(role),
  nick_name = VALUES(nick_name),
  status = VALUES(status),
  updated_at = NOW();

-- 会话初始化
INSERT INTO im_conversation (id, conversation_type, biz_key, biz_id, conversation_name, avatar_url, last_message_id, last_message_preview, last_message_time, status, created_at, updated_at)
VALUES
  (30001, 1, '10001_10002', NULL, 'Echo用户02', NULL, 40002, '今晚同步一下接口设计', NOW(), 1, NOW(), NOW()),
  (30002, 2, 'group_20001', 20001, 'Echo 项目讨论群', NULL, 40003, '大家晚上好', NOW(), 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  conversation_name = VALUES(conversation_name),
  last_message_id = VALUES(last_message_id),
  last_message_preview = VALUES(last_message_preview),
  last_message_time = VALUES(last_message_time),
  status = VALUES(status),
  updated_at = NOW();

INSERT INTO im_conversation_user (id, conversation_id, user_id, unread_count, last_read_seq, is_top, is_mute, deleted, created_at, updated_at)
VALUES
  (1, 30001, 10001, 0, 2, 1, 0, 0, NOW(), NOW()),
  (2, 30001, 10002, 0, 2, 0, 0, 0, NOW(), NOW()),
  (3, 30002, 10001, 0, 1, 0, 0, 0, NOW(), NOW()),
  (4, 30002, 10002, 1, 0, 0, 0, 0, NOW(), NOW()),
  (5, 30002, 10003, 1, 0, 0, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  unread_count = VALUES(unread_count),
  last_read_seq = VALUES(last_read_seq),
  is_top = VALUES(is_top),
  is_mute = VALUES(is_mute),
  deleted = VALUES(deleted),
  updated_at = NOW();

-- 文件初始化
INSERT INTO im_file (id, owner_user_id, biz_type, storage_type, bucket_name, object_key, file_name, file_ext, content_type, file_size, md5, url, status, created_at, updated_at)
VALUES
  (50001, 10001, 2, 'local', NULL, 'upload/demo/welcome.png', 'welcome.png', 'png', 'image/png', 102400, NULL, '/upload/demo/welcome.png', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  file_name = VALUES(file_name),
  content_type = VALUES(content_type),
  file_size = VALUES(file_size),
  url = VALUES(url),
  status = VALUES(status),
  updated_at = NOW();

-- 消息初始化
INSERT INTO im_message (id, conversation_id, conversation_type, seq_no, client_msg_id, from_user_id, to_user_id, group_id, msg_type, content, extra_json, file_id, send_status, sent_at, created_at, updated_at)
VALUES
  (40001, 30001, 1, 1, 'seed-cmsg-40001', 10001, 10002, NULL, 1, '你好，欢迎测试 EchoIM', NULL, NULL, 1, NOW(), NOW(), NOW()),
  (40002, 30001, 1, 2, 'seed-cmsg-40002', 10002, 10001, NULL, 1, '今晚同步一下接口设计', NULL, NULL, 1, NOW(), NOW(), NOW()),
  (40003, 30002, 2, 1, 'seed-cmsg-40003', 10001, NULL, 20001, 1, '大家晚上好', NULL, NULL, 1, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  send_status = VALUES(send_status),
  updated_at = NOW();

-- 消息回执初始化
INSERT INTO im_message_receipt (id, message_id, conversation_id, user_id, receipt_type, receipt_at)
VALUES
  (1, 40001, 30001, 10002, 1, NOW()),
  (2, 40001, 30001, 10002, 2, NOW()),
  (3, 40002, 30001, 10001, 1, NOW())
ON DUPLICATE KEY UPDATE
  receipt_at = VALUES(receipt_at);

SET FOREIGN_KEY_CHECKS = 1;
