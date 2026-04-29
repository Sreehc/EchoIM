ALTER TABLE im_group
  ADD COLUMN conversation_type TINYINT NOT NULL DEFAULT 2 COMMENT '2群聊 3频道' AFTER owner_user_id;

ALTER TABLE im_conversation_user
  ADD COLUMN is_archived TINYINT NOT NULL DEFAULT 0 COMMENT '0否 1是' AFTER is_mute,
  ADD COLUMN manual_unread TINYINT NOT NULL DEFAULT 0 COMMENT '0否 1是' AFTER is_archived;

ALTER TABLE im_conversation_user
  ADD KEY idx_user_id_top (user_id, is_archived, is_top),
  ADD KEY idx_user_manual_unread (user_id, manual_unread);

UPDATE im_group g
JOIN im_conversation c
  ON c.biz_id = g.id
 AND c.conversation_type IN (2, 3)
SET g.conversation_type = c.conversation_type
WHERE g.conversation_type = 2;
