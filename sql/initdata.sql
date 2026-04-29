-- EchoIM test data seed
-- Run after sql/init.sql.
-- All demo user passwords are: 123456

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET time_zone = '+08:00';
USE echoim;

SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

-- System records.
INSERT INTO sys_admin_user (id, username, password_hash, nickname, role_code, status, created_at, updated_at)
VALUES
  (1, 'admin', '$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y', '系统管理员', 'super_admin', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  nickname = VALUES(nickname),
  role_code = VALUES(role_code),
  status = VALUES(status),
  updated_at = NOW();

INSERT INTO sys_config (id, config_key, config_value, config_name, remark, status, created_at, updated_at)
VALUES
  (1, 'file.max-size-mb', '50', '文件上传大小限制', '上传文件最大 50MB', 1, NOW(), NOW()),
  (2, 'message.recall-seconds', '120', '消息撤回时间限制', '发送后 120 秒内可撤回', 1, NOW(), NOW()),
  (3, 'register.enabled', 'true', '是否允许注册', '控制前台注册开关', 1, NOW(), NOW()),
  (4, 'demo.seed-version', '2026-04-29', '演示数据版本', '用于确认 initdata.sql 是否已经执行', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  config_name = VALUES(config_name),
  remark = VALUES(remark),
  status = VALUES(status),
  updated_at = NOW();

INSERT INTO sys_version (id, version_code, version_name, platform, release_note, force_update, gray_percent, publish_status, published_at, created_at, updated_at)
VALUES
  (1, 'v0.1.0', 'EchoIM MVP', 'web', '本地演示版本，包含登录、会话列表、单聊、群聊、实时消息和基础设置。', 0, 100, 1, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
  version_name = VALUES(version_name),
  release_note = VALUES(release_note),
  gray_percent = VALUES(gray_percent),
  publish_status = VALUES(publish_status),
  published_at = VALUES(published_at),
  updated_at = NOW();

INSERT INTO sys_beauty_no (id, beauty_no, bind_user_id, level_type, status, remark, created_at, updated_at)
VALUES
  (1, '10000', NULL, 2, 1, '演示稀有靓号', NOW(), NOW()),
  (2, '88888', NULL, 3, 1, '演示高价值靓号', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  bind_user_id = VALUES(bind_user_id),
  level_type = VALUES(level_type),
  status = VALUES(status),
  remark = VALUES(remark),
  updated_at = NOW();

-- Remove only deterministic seed data so the script can be re-run safely.
DELETE FROM im_message_reaction
WHERE id BETWEEN 70001 AND 70020
   OR message_id BETWEEN 40001 AND 40050
   OR user_id BETWEEN 10001 AND 10006;

DELETE FROM im_call_session
WHERE id BETWEEN 71001 AND 71020
   OR conversation_id BETWEEN 30001 AND 30010
   OR caller_user_id BETWEEN 10001 AND 10006
   OR callee_user_id BETWEEN 10001 AND 10006;

DELETE FROM im_message_receipt
WHERE message_id BETWEEN 40001 AND 40040
   OR conversation_id BETWEEN 30001 AND 30010
   OR user_id BETWEEN 10001 AND 10006;

DELETE FROM im_message
WHERE id BETWEEN 40001 AND 40050
   OR client_msg_id LIKE 'seed-%'
   OR conversation_id BETWEEN 30001 AND 30010;

DELETE FROM im_file
WHERE id BETWEEN 50001 AND 50010
   OR object_key LIKE 'seed/%';

DELETE FROM im_conversation_user
WHERE conversation_id BETWEEN 30001 AND 30010
   OR user_id BETWEEN 10001 AND 10006;

DELETE FROM im_conversation
WHERE id BETWEEN 30001 AND 30010
   OR biz_key IN ('10001_10002', '10001_10003', '10001_10004', '10001_10005', 'group_20001');

DELETE FROM im_group_member
WHERE group_id BETWEEN 20001 AND 20010
   OR user_id BETWEEN 10001 AND 10006;

DELETE FROM im_group
WHERE id BETWEEN 20001 AND 20010
   OR group_no IN ('G20001', 'G20002');

DELETE FROM im_friend
WHERE user_id BETWEEN 10001 AND 10006
   OR friend_user_id BETWEEN 10001 AND 10006;

DELETE FROM im_friend_request
WHERE from_user_id BETWEEN 10001 AND 10006
   OR to_user_id BETWEEN 10001 AND 10006;

DELETE FROM im_user
WHERE id BETWEEN 10001 AND 10006
   OR user_no IN ('E10001', 'E10002', 'E10003', 'E10004', 'E10005', 'E10006')
   OR username IN ('echo_demo_01', 'echo_demo_02', 'echo_demo_03', 'echo_demo_04', 'echo_demo_05', 'echo_demo_06');

-- Demo users.
INSERT INTO im_user (id, user_no, username, password_hash, nickname, avatar_url, gender, phone, email, signature, status, last_login_at, created_at, updated_at)
VALUES
  (10001, 'E10001', 'echo_demo_01', '$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y', 'Echo用户01', NULL, 1, '13800000001', 'echo01@example.com', '正在把 EchoIM 调到顺手。', 1, NOW(), NOW(), NOW()),
  (10002, 'E10002', 'echo_demo_02', '$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y', '周序', NULL, 1, '13800000002', 'zhouxu@example.com', '产品、接口和体验一起推进。', 1, NOW(), NOW(), NOW()),
  (10003, 'E10003', 'echo_demo_03', '$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y', '宋眠', NULL, 2, '13800000003', 'songmian@example.com', '关注细节和可用性。', 1, NOW(), NOW(), NOW()),
  (10004, 'E10004', 'echo_demo_04', '$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y', '裴见', NULL, 1, '13800000004', 'peijian@example.com', '负责联调与排查。', 1, NOW(), NOW(), NOW()),
  (10005, 'E10005', 'echo_demo_05', '$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y', '程原', NULL, 1, '13800000005', 'chengyuan@example.com', '把复杂事情讲清楚。', 1, NOW(), NOW(), NOW()),
  (10006, 'E10006', 'echo_demo_06', '$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y', '沈曜', NULL, 1, '13800000006', 'shenyao@example.com', '先验证，再上线。', 1, NOW(), NOW(), NOW());

-- Friend requests and accepted friend relations.
INSERT INTO im_friend_request (id, from_user_id, to_user_id, apply_msg, status, handled_by, handled_at, created_at, updated_at)
VALUES
  (1, 10002, 10001, '我这边需要一起确认聊天页交互。', 1, 10001, TIMESTAMPADD(DAY, -7, NOW()), TIMESTAMPADD(DAY, -7, NOW()), TIMESTAMPADD(DAY, -7, NOW())),
  (2, 10003, 10001, '我想加入测试账号列表。', 1, 10001, TIMESTAMPADD(DAY, -6, NOW()), TIMESTAMPADD(DAY, -6, NOW()), TIMESTAMPADD(DAY, -6, NOW())),
  (3, 10004, 10001, '方便一起联调消息和通知。', 1, 10001, TIMESTAMPADD(DAY, -5, NOW()), TIMESTAMPADD(DAY, -5, NOW()), TIMESTAMPADD(DAY, -5, NOW()));

INSERT INTO im_friend (id, user_id, friend_user_id, remark, status, created_at, updated_at)
VALUES
  (1, 10001, 10002, '周序', 1, TIMESTAMPADD(DAY, -7, NOW()), NOW()),
  (2, 10002, 10001, 'Echo用户01', 1, TIMESTAMPADD(DAY, -7, NOW()), NOW()),
  (3, 10001, 10003, '宋眠', 1, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (4, 10003, 10001, 'Echo用户01', 1, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (5, 10001, 10004, '裴见', 1, TIMESTAMPADD(DAY, -5, NOW()), NOW()),
  (6, 10004, 10001, 'Echo用户01', 1, TIMESTAMPADD(DAY, -5, NOW()), NOW()),
  (7, 10001, 10005, '程原', 1, TIMESTAMPADD(DAY, -4, NOW()), NOW()),
  (8, 10005, 10001, 'Echo用户01', 1, TIMESTAMPADD(DAY, -4, NOW()), NOW()),
  (9, 10001, 10006, '沈曜', 1, TIMESTAMPADD(DAY, -3, NOW()), NOW()),
  (10, 10006, 10001, 'Echo用户01', 1, TIMESTAMPADD(DAY, -3, NOW()), NOW());

-- Group and members.
INSERT INTO im_group (id, group_no, group_name, owner_user_id, conversation_type, avatar_url, notice, status, created_at, updated_at)
VALUES
  (20001, 'G20001', 'EchoIM 工作台', 10001, 2, NULL, '用于测试群聊、未读、通知和长消息展示。', 1, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (20002, 'G20002', 'EchoIM 发布频道', 10001, 3, NULL, '仅创建者可发送消息，用于测试频道消息样式和查看人数。', 1, TIMESTAMPADD(DAY, -2, NOW()), NOW());

INSERT INTO im_group_member (id, group_id, user_id, role, nick_name, join_source, join_at, status, updated_at)
VALUES
  (1, 20001, 10001, 1, 'Echo用户01', 1, TIMESTAMPADD(DAY, -6, NOW()), 1, NOW()),
  (2, 20001, 10002, 3, '周序', 2, TIMESTAMPADD(DAY, -6, NOW()), 1, NOW()),
  (3, 20001, 10003, 2, '宋眠', 2, TIMESTAMPADD(DAY, -6, NOW()), 1, NOW()),
  (4, 20001, 10004, 2, '裴见', 2, TIMESTAMPADD(DAY, -6, NOW()), 1, NOW()),
  (5, 20001, 10005, 2, '程原', 2, TIMESTAMPADD(DAY, -6, NOW()), 1, NOW()),
  (6, 20001, 10006, 2, '沈曜', 2, TIMESTAMPADD(DAY, -6, NOW()), 1, NOW()),
  (7, 20002, 10001, 1, 'Echo用户01', 1, TIMESTAMPADD(DAY, -2, NOW()), 1, NOW()),
  (8, 20002, 10002, 2, '周序', 2, TIMESTAMPADD(DAY, -2, NOW()), 1, NOW()),
  (9, 20002, 10003, 2, '宋眠', 2, TIMESTAMPADD(DAY, -2, NOW()), 1, NOW()),
  (10, 20002, 10004, 2, '裴见', 2, TIMESTAMPADD(DAY, -2, NOW()), 1, NOW()),
  (11, 20002, 10005, 2, '程原', 2, TIMESTAMPADD(DAY, -2, NOW()), 1, NOW()),
  (12, 20002, 10006, 2, '沈曜', 2, TIMESTAMPADD(DAY, -2, NOW()), 1, NOW());

-- Conversations.
INSERT INTO im_conversation (id, conversation_type, biz_key, biz_id, conversation_name, avatar_url, last_message_id, last_message_preview, last_message_time, status, created_at, updated_at)
VALUES
  (30001, 1, '10001_10002', NULL, '周序', NULL, 40008, '我刚发了一条新消息，前端应该能显示未读。', TIMESTAMPADD(MINUTE, -3, NOW()), 1, TIMESTAMPADD(DAY, -7, NOW()), NOW()),
  (30002, 2, 'group_20001', 20001, 'EchoIM 工作台', NULL, 40022, '通知条和空态都可以按这个数据回归。', TIMESTAMPADD(MINUTE, -8, NOW()), 1, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (30003, 1, '10001_10003', NULL, '宋眠', NULL, 40028, '浅色主题里这段摘要应该也清楚。', TIMESTAMPADD(HOUR, -2, NOW()), 1, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (30004, 1, '10001_10004', NULL, '裴见', NULL, 40034, '我下午会继续压测 WebSocket。', TIMESTAMPADD(HOUR, -5, NOW()), 1, TIMESTAMPADD(DAY, -5, NOW()), NOW()),
  (30005, 1, '10001_10005', NULL, '程原', NULL, 40038, '收到，等你确认后我再发第二版。', TIMESTAMPADD(DAY, -1, NOW()), 1, TIMESTAMPADD(DAY, -4, NOW()), NOW()),
  (30006, 3, 'channel_20002', 20002, 'EchoIM 发布频道', NULL, 40042, '今晚会把新一版消息状态展示合并到主分支。', TIMESTAMPADD(MINUTE, -16, NOW()), 1, TIMESTAMPADD(DAY, -2, NOW()), NOW());

INSERT INTO im_conversation_user (id, conversation_id, user_id, unread_count, last_read_seq, is_top, is_mute, is_archived, manual_unread, deleted, created_at, updated_at)
VALUES
  (1, 30001, 10001, 2, 6, 1, 0, 0, 0, 0, TIMESTAMPADD(DAY, -7, NOW()), NOW()),
  (2, 30001, 10002, 0, 8, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -7, NOW()), NOW()),
  (3, 30002, 10001, 4, 8, 0, 0, 0, 1, 0, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (4, 30002, 10002, 0, 12, 1, 0, 0, 0, 0, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (5, 30002, 10003, 1, 11, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (6, 30002, 10004, 2, 10, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (7, 30002, 10005, 0, 12, 0, 1, 0, 0, 0, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (8, 30002, 10006, 0, 12, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (9, 30003, 10001, 0, 6, 0, 0, 1, 0, 0, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (10, 30003, 10003, 0, 6, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -6, NOW()), NOW()),
  (11, 30004, 10001, 1, 5, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -5, NOW()), NOW()),
  (12, 30004, 10004, 0, 6, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -5, NOW()), NOW()),
  (13, 30005, 10001, 0, 4, 0, 1, 0, 0, 0, TIMESTAMPADD(DAY, -4, NOW()), NOW()),
  (14, 30005, 10005, 0, 4, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -4, NOW()), NOW()),
  (15, 30006, 10001, 0, 4, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -2, NOW()), NOW()),
  (16, 30006, 10002, 0, 4, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -2, NOW()), NOW()),
  (17, 30006, 10003, 1, 3, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -2, NOW()), NOW()),
  (18, 30006, 10004, 0, 4, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -2, NOW()), NOW()),
  (19, 30006, 10005, 2, 2, 0, 1, 0, 0, 0, TIMESTAMPADD(DAY, -2, NOW()), NOW()),
  (20, 30006, 10006, 3, 1, 0, 0, 0, 0, 0, TIMESTAMPADD(DAY, -2, NOW()), NOW());

-- Text-only messages. Avoid file_id here so local OSS/minio config is not required for message loading.
INSERT INTO im_message (id, conversation_id, conversation_type, seq_no, client_msg_id, from_user_id, to_user_id, group_id, msg_type, content, extra_json, file_id, send_status, sent_at, created_at, updated_at)
VALUES
  (40001, 30001, 1, 1, 'seed-cmsg-40001', 10001, 10002, NULL, 1, '早上好，今天主要看聊天工作台这块。', NULL, NULL, 1, TIMESTAMPADD(DAY, -2, TIMESTAMPADD(MINUTE, -36, NOW())), NOW(), NOW()),
  (40002, 30001, 1, 2, 'seed-cmsg-40002', 10002, 10001, NULL, 1, '我先确认接口返回的中文和未读数。', NULL, NULL, 1, TIMESTAMPADD(DAY, -2, TIMESTAMPADD(MINUTE, -34, NOW())), NOW(), NOW()),
  (40003, 30001, 1, 3, 'seed-cmsg-40003', 10001, 10002, NULL, 1, '如果看到乱码，优先按数据库字符集和连接参数排查。', NULL, NULL, 1, TIMESTAMPADD(DAY, -2, TIMESTAMPADD(MINUTE, -31, NOW())), NOW(), NOW()),
  (40004, 30001, 1, 4, 'seed-cmsg-40004', 10002, 10001, NULL, 1, '明白。我会用中文消息、长摘要和通知入口一起测。', NULL, NULL, 1, TIMESTAMPADD(DAY, -2, TIMESTAMPADD(MINUTE, -28, NOW())), NOW(), NOW()),
  (40005, 30001, 1, 5, 'seed-cmsg-40005', 10001, 10002, NULL, 1, '左侧列表点击后再显示聊天内容，这个行为也要保留。', NULL, NULL, 1, TIMESTAMPADD(MINUTE, -18, NOW()), NOW(), NOW()),
  (40006, 30001, 1, 6, 'seed-cmsg-40006', 10002, 10001, NULL, 1, '收到，我会注意不要自动打开最近会话。', NULL, NULL, 1, TIMESTAMPADD(MINUTE, -14, NOW()), NOW(), NOW()),
  (40007, 30001, 1, 7, 'seed-cmsg-40007', 10002, 10001, NULL, 1, '通知授权那块我也会点一下，确认浏览器弹窗能出来。', NULL, NULL, 1, TIMESTAMPADD(MINUTE, -7, NOW()), NOW(), NOW()),
  (40008, 30001, 1, 8, 'seed-cmsg-40008', 10002, 10001, NULL, 1, '我刚发了一条新消息，前端应该能显示未读。', NULL, NULL, 1, TIMESTAMPADD(MINUTE, -3, NOW()), NOW(), NOW()),

  (40011, 30002, 2, 1, 'seed-cmsg-40011', 10001, NULL, 20001, 6, 'EchoIM 工作台创建成功', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(MINUTE, -80, NOW())), NOW(), NOW()),
  (40012, 30002, 2, 2, 'seed-cmsg-40012', 10002, NULL, 20001, 1, '大家今天重点测三件事：会话列表、消息滚动、桌面通知。', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(MINUTE, -78, NOW())), NOW(), NOW()),
  (40013, 30002, 2, 3, 'seed-cmsg-40013', 10003, NULL, 20001, 1, '我会看浅色和深色模式下的文字对比。', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(MINUTE, -76, NOW())), NOW(), NOW()),
  (40014, 30002, 2, 4, 'seed-cmsg-40014', 10004, NULL, 20001, 1, '我负责实时消息和重连场景。', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(MINUTE, -75, NOW())), NOW(), NOW()),
  (40015, 30002, 2, 5, 'seed-cmsg-40015', 10005, NULL, 20001, 1, '长一点的消息也要留一条：如果用户打开聊天页后没有选中会话，中间区域应该保持空白背景，而不是显示旧对话或输入框。', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(MINUTE, -72, NOW())), NOW(), NOW()),
  (40016, 30002, 2, 6, 'seed-cmsg-40016', 10006, NULL, 20001, 1, '我这里看到列表摘要正常，没有乱码。', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(MINUTE, -70, NOW())), NOW(), NOW()),
  (40017, 30002, 2, 7, 'seed-cmsg-40017', 10001, NULL, 20001, 1, '辛苦。等会我重新执行 initdata.sql，再登录 echo_demo_01 验证。', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(MINUTE, -68, NOW())), NOW(), NOW()),
  (40018, 30002, 2, 8, 'seed-cmsg-40018', 10002, NULL, 20001, 1, '如果需要测试通知，可以先允许浏览器桌面通知。', NULL, NULL, 1, TIMESTAMPADD(MINUTE, -35, NOW()), NOW(), NOW()),
  (40019, 30002, 2, 9, 'seed-cmsg-40019', 10003, NULL, 20001, 1, '我切换到浅色主题后再看一下输入区。', NULL, NULL, 1, TIMESTAMPADD(MINUTE, -28, NOW()), NOW(), NOW()),
  (40020, 30002, 2, 10, 'seed-cmsg-40020', 10004, NULL, 20001, 1, 'WebSocket 连接恢复后，新消息会继续追加。', NULL, NULL, 1, TIMESTAMPADD(MINUTE, -20, NOW()), NOW(), NOW()),
  (40021, 30002, 2, 11, 'seed-cmsg-40021', 10006, NULL, 20001, 1, '我看了一下按钮尺寸，现在更协调。', NULL, NULL, 1, TIMESTAMPADD(MINUTE, -13, NOW()), NOW(), NOW()),
  (40022, 30002, 2, 12, 'seed-cmsg-40022', 10002, NULL, 20001, 1, '通知条和空态都可以按这个数据回归。', NULL, NULL, 1, TIMESTAMPADD(MINUTE, -8, NOW()), NOW(), NOW()),

  (40023, 30003, 1, 1, 'seed-cmsg-40023', 10003, 10001, NULL, 1, '我刚整理了一版中文文案，你看看语气够不够自然。', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, -3, NOW())), NOW(), NOW()),
  (40024, 30003, 1, 2, 'seed-cmsg-40024', 10001, 10003, NULL, 1, '整体可以，按钮文案再短一点。', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, -2, NOW())), NOW(), NOW()),
  (40025, 30003, 1, 3, 'seed-cmsg-40025', 10003, 10001, NULL, 1, '那我把通知提示改成“不错过新消息”。', NULL, NULL, 1, TIMESTAMPADD(HOUR, -3, NOW()), NOW(), NOW()),
  (40026, 30003, 1, 4, 'seed-cmsg-40026', 10001, 10003, NULL, 1, '可以，正文说明浏览器会弹出确认就够了。', NULL, NULL, 1, TIMESTAMPADD(HOUR, -2, TIMESTAMPADD(MINUTE, -45, NOW())), NOW(), NOW()),
  (40027, 30003, 1, 5, 'seed-cmsg-40027', 10003, 10001, NULL, 1, '深色背景下不要太灰，正文至少要看清。', NULL, NULL, 1, TIMESTAMPADD(HOUR, -2, TIMESTAMPADD(MINUTE, -20, NOW())), NOW(), NOW()),
  (40028, 30003, 1, 6, 'seed-cmsg-40028', 10003, 10001, NULL, 1, '浅色主题里这段摘要应该也清楚。', NULL, NULL, 1, TIMESTAMPADD(HOUR, -2, NOW()), NOW(), NOW()),

  (40029, 30004, 1, 1, 'seed-cmsg-40029', 10004, 10001, NULL, 1, '本地环境我已经起来了。', NULL, NULL, 1, TIMESTAMPADD(DAY, -2, NOW()), NOW(), NOW()),
  (40030, 30004, 1, 2, 'seed-cmsg-40030', 10001, 10004, NULL, 1, '麻烦看一下消息接口分页。', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, -6, NOW())), NOW(), NOW()),
  (40031, 30004, 1, 3, 'seed-cmsg-40031', 10004, 10001, NULL, 1, '分页是倒序查再反转，首屏会显示最新 50 条。', NULL, NULL, 1, TIMESTAMPADD(HOUR, -7, NOW()), NOW(), NOW()),
  (40032, 30004, 1, 4, 'seed-cmsg-40032', 10001, 10004, NULL, 1, '未读数打开会话后会清零。', NULL, NULL, 1, TIMESTAMPADD(HOUR, -6, NOW()), NOW(), NOW()),
  (40033, 30004, 1, 5, 'seed-cmsg-40033', 10004, 10001, NULL, 1, '我再补一条用来测试通知。', NULL, NULL, 1, TIMESTAMPADD(HOUR, -5, TIMESTAMPADD(MINUTE, -10, NOW())), NOW(), NOW()),
  (40034, 30004, 1, 6, 'seed-cmsg-40034', 10004, 10001, NULL, 1, '我下午会继续压测 WebSocket。', NULL, NULL, 1, TIMESTAMPADD(HOUR, -5, NOW()), NOW(), NOW()),

  (40035, 30005, 1, 1, 'seed-cmsg-40035', 10001, 10005, NULL, 1, '这轮先不要加太多设置项，保证主链路稳定。', NULL, NULL, 1, TIMESTAMPADD(DAY, -2, NOW()), NOW(), NOW()),
  (40036, 30005, 1, 2, 'seed-cmsg-40036', 10005, 10001, NULL, 1, '同意，先把登录、列表、打开会话、发消息测通。', NULL, NULL, 1, TIMESTAMPADD(DAY, -2, TIMESTAMPADD(HOUR, 1, NOW())), NOW(), NOW()),
  (40037, 30005, 1, 3, 'seed-cmsg-40037', 10001, 10005, NULL, 1, '我会用这个脚本重置演示数据。', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, -2, NOW())), NOW(), NOW()),
  (40038, 30005, 1, 4, 'seed-cmsg-40038', 10005, 10001, NULL, 1, '收到，等你确认后我再发第二版。', NULL, NULL, 1, TIMESTAMPADD(DAY, -1, NOW()), NOW(), NOW()),

  (40039, 30006, 3, 1, 'seed-cmsg-40039', 10001, NULL, 20002, 1, '这个频道只保留关键发布内容，成员默认只读。', NULL, NULL, 1, TIMESTAMPADD(HOUR, -4, NOW()), NOW(), NOW()),
  (40040, 30006, 3, 2, 'seed-cmsg-40040', 10001, NULL, 20002, 1, '频道消息旁边会显示真实查看人数，不显示群聊那种双对号。', NULL, NULL, 1, TIMESTAMPADD(HOUR, -3, NOW()), NOW(), NOW()),
  (40041, 30006, 3, 3, 'seed-cmsg-40041', 10001, NULL, 20002, 1, '如果你是创建者，输入区保持可发；普通成员只会看到只读提示。', NULL, NULL, 1, TIMESTAMPADD(HOUR, -2, NOW()), NOW(), NOW()),
  (40042, 30006, 3, 4, 'seed-cmsg-40042', 10001, NULL, 20002, 1, '今晚会把新一版消息状态展示合并到主分支。', NULL, NULL, 1, TIMESTAMPADD(MINUTE, -16, NOW()), NOW(), NOW());

INSERT INTO im_message_reaction (id, message_id, user_id, emoji, created_at, updated_at)
VALUES
  (70001, 40008, 10001, '👍', TIMESTAMPADD(MINUTE, -2, NOW()), NOW()),
  (70002, 40018, 10001, '🔥', TIMESTAMPADD(MINUTE, -30, NOW()), NOW()),
  (70003, 40022, 10003, '❤️', TIMESTAMPADD(MINUTE, -6, NOW()), NOW()),
  (70004, 40042, 10002, '👏', TIMESTAMPADD(MINUTE, -10, NOW()), NOW());

INSERT INTO im_call_session (id, conversation_id, call_type, caller_user_id, callee_user_id, status, started_at, answered_at, ended_at, end_reason, created_at, updated_at)
VALUES
  (71001, 30001, 'audio', 10001, 10002, 'ended', TIMESTAMPADD(HOUR, -20, NOW()), TIMESTAMPADD(HOUR, -20, TIMESTAMPADD(SECOND, 8, NOW())), TIMESTAMPADD(HOUR, -20, TIMESTAMPADD(MINUTE, 6, NOW())), 'hangup', NOW(), NOW()),
  (71002, 30004, 'audio', 10004, 10001, 'missed', TIMESTAMPADD(HOUR, -9, NOW()), NULL, TIMESTAMPADD(HOUR, -9, TIMESTAMPADD(SECOND, 30, NOW())), 'timeout', NOW(), NOW());

-- Receipts for single-chat messages sent by echo_demo_01.
INSERT INTO im_message_receipt (id, message_id, conversation_id, user_id, receipt_type, receipt_at)
VALUES
  (1, 40001, 30001, 10002, 1, TIMESTAMPADD(DAY, -2, TIMESTAMPADD(MINUTE, -35, NOW()))),
  (2, 40001, 30001, 10002, 2, TIMESTAMPADD(DAY, -2, TIMESTAMPADD(MINUTE, -34, NOW()))),
  (3, 40003, 30001, 10002, 1, TIMESTAMPADD(DAY, -2, TIMESTAMPADD(MINUTE, -30, NOW()))),
  (4, 40003, 30001, 10002, 2, TIMESTAMPADD(DAY, -2, TIMESTAMPADD(MINUTE, -29, NOW()))),
  (5, 40005, 30001, 10002, 1, TIMESTAMPADD(MINUTE, -17, NOW())),
  (6, 40005, 30001, 10002, 2, TIMESTAMPADD(MINUTE, -16, NOW())),
  (7, 40024, 30003, 10003, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, -2, NOW()))),
  (8, 40024, 30003, 10003, 2, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, -2, NOW()))),
  (9, 40026, 30003, 10003, 1, TIMESTAMPADD(HOUR, -2, TIMESTAMPADD(MINUTE, -44, NOW()))),
  (10, 40026, 30003, 10003, 2, TIMESTAMPADD(HOUR, -2, TIMESTAMPADD(MINUTE, -43, NOW()))),
  (11, 40030, 30004, 10004, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, -6, NOW()))),
  (12, 40030, 30004, 10004, 2, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, -6, NOW()))),
  (13, 40032, 30004, 10004, 1, TIMESTAMPADD(HOUR, -6, NOW())),
  (14, 40037, 30005, 10005, 1, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, -2, NOW()))),
  (15, 40037, 30005, 10005, 2, TIMESTAMPADD(DAY, -1, TIMESTAMPADD(HOUR, -2, NOW())));

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;

-- Quick checks:
-- SELECT id, username, nickname FROM im_user WHERE id BETWEEN 10001 AND 10006;
-- SELECT conversation_id, user_id, unread_count, last_read_seq, is_top FROM im_conversation_user WHERE user_id = 10001 ORDER BY is_top DESC, conversation_id;
