-- MySQL dump 10.13  Distrib 8.0.44, for Linux (aarch64)
--
-- Host: localhost    Database: echoim
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `echoim`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `echoim` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `echoim`;

--
-- Table structure for table `im_conversation`
--

DROP TABLE IF EXISTS `im_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `im_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¼šè¯ID',
  `conversation_type` tinyint NOT NULL COMMENT '1å•èŠ 2ç¾¤èŠ',
  `biz_key` varchar(64) NOT NULL COMMENT 'å•èŠä¸ºè¾ƒå°ç”¨æˆ·ID_è¾ƒå¤§ç”¨æˆ·IDï¼Œç¾¤èŠä¸ºgroup_{groupId}',
  `biz_id` bigint DEFAULT NULL COMMENT 'ä¸šåŠ¡IDï¼Œç¾¤èŠæ—¶ä¸ºgroupId',
  `conversation_name` varchar(100) DEFAULT NULL COMMENT 'ä¼šè¯åç§°',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT 'ä¼šè¯å¤´åƒ',
  `last_message_id` bigint DEFAULT NULL COMMENT 'æœ€åŽæ¶ˆæ¯ID',
  `last_message_preview` varchar(500) DEFAULT NULL COMMENT 'æœ€åŽæ¶ˆæ¯æ‘˜è¦',
  `last_message_time` datetime DEFAULT NULL COMMENT 'æœ€åŽæ¶ˆæ¯æ—¶é—´',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1æ­£å¸¸ 2åˆ é™¤',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_biz_key` (`conversation_type`,`biz_key`),
  KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB AUTO_INCREMENT=30011 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ä¼šè¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `im_conversation`
--

LOCK TABLES `im_conversation` WRITE;
/*!40000 ALTER TABLE `im_conversation` DISABLE KEYS */;
INSERT INTO `im_conversation` VALUES (30001,1,'10001_10002',NULL,'Echoç”¨æˆ·02',NULL,40030,'ws-1777110706948','2026-04-25 09:51:46',1,'2026-04-22 13:16:47','2026-04-25 09:51:46'),(30002,2,'group_20001',20001,'Echo é¡¹ç›®è®¨è®ºç¾¤',NULL,40003,'å¤§å®¶æ™šä¸Šå¥½','2026-04-22 14:02:34',1,'2026-04-22 13:16:47','2026-04-22 14:02:34'),(30003,1,'10005_10006',NULL,'10005_10006',NULL,40004,'phase2 hello','2026-04-22 14:51:41',1,'2026-04-22 14:51:41','2026-04-22 14:51:41'),(30004,1,'10007_10008',NULL,'10007_10008',NULL,40006,'hello phase3 1776910591606','2026-04-23 02:16:32',1,'2026-04-23 02:16:32','2026-04-23 02:16:32'),(30005,1,'10009_10010',NULL,'10009_10010',NULL,40009,'offline sync msg 2','2026-04-23 13:12:37',1,'2026-04-23 13:12:37','2026-04-23 13:12:37'),(30006,2,'group_20002',20002,'第五阶段测试群',NULL,NULL,NULL,NULL,1,'2026-04-24 09:29:17','2026-04-24 09:29:17'),(30007,2,'group_20003',20003,'WS群聊测试',NULL,40010,'hello group','2026-04-24 09:29:47',1,'2026-04-24 09:29:47','2026-04-24 09:29:47'),(30008,2,'group_20004',20004,'第五阶段群发验证',NULL,40011,'group ok','2026-04-24 09:32:04',1,'2026-04-24 09:32:03','2026-04-24 09:32:04'),(30009,2,'group_20005',20005,'移除摘要验证',NULL,40013,'after remove hidden','2026-04-24 09:34:06',1,'2026-04-24 09:34:05','2026-04-24 09:34:06'),(30010,2,'group_20006',20006,'移除摘要验证2',NULL,40015,'after remove hidden','2026-04-24 09:36:41',1,'2026-04-24 09:36:40','2026-04-24 09:36:41');
/*!40000 ALTER TABLE `im_conversation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `im_conversation_user`
--

DROP TABLE IF EXISTS `im_conversation_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `im_conversation_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç”¨æˆ·ä¼šè¯ID',
  `conversation_id` bigint NOT NULL COMMENT 'ä¼šè¯ID',
  `user_id` bigint NOT NULL COMMENT 'ç”¨æˆ·ID',
  `unread_count` int NOT NULL DEFAULT '0' COMMENT 'æœªè¯»æ•°',
  `last_read_seq` bigint NOT NULL DEFAULT '0' COMMENT 'æœ€åŽå·²è¯»åºå·',
  `is_top` tinyint NOT NULL DEFAULT '0' COMMENT '0å¦ 1æ˜¯',
  `is_mute` tinyint NOT NULL DEFAULT '0' COMMENT '0å¦ 1æ˜¯',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0å¦ 1æ˜¯',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_user` (`conversation_id`,`user_id`),
  KEY `idx_user_id_top` (`user_id`,`is_top`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ç”¨æˆ·ä¼šè¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `im_conversation_user`
--

LOCK TABLES `im_conversation_user` WRITE;
/*!40000 ALTER TABLE `im_conversation_user` DISABLE KEYS */;
INSERT INTO `im_conversation_user` VALUES (1,30001,10001,0,17,1,0,0,'2026-04-22 13:16:47','2026-04-25 09:51:59'),(2,30001,10002,0,17,0,0,0,'2026-04-22 13:16:47','2026-04-25 09:51:47'),(3,30002,10001,0,1,0,0,0,'2026-04-22 13:16:47','2026-04-22 14:02:34'),(4,30002,10002,1,0,0,0,0,'2026-04-22 13:16:47','2026-04-22 14:02:34'),(5,30002,10003,1,0,0,0,0,'2026-04-22 13:16:47','2026-04-22 14:02:34'),(6,30003,10005,0,0,0,0,0,'2026-04-22 14:51:41','2026-04-22 14:51:41'),(7,30003,10006,0,0,0,0,0,'2026-04-22 14:51:41','2026-04-22 14:51:41'),(8,30004,10007,0,0,0,0,0,'2026-04-23 02:16:32','2026-04-23 02:16:32'),(9,30004,10008,0,2,0,0,0,'2026-04-23 02:16:32','2026-04-23 02:16:32'),(10,30005,10009,0,0,0,0,0,'2026-04-23 13:12:37','2026-04-23 13:12:37'),(11,30005,10010,0,3,0,0,0,'2026-04-23 13:12:37','2026-04-23 13:12:37'),(12,30006,10001,0,0,1,1,1,'2026-04-24 09:29:17','2026-04-24 09:29:17'),(13,30006,10002,0,0,0,0,0,'2026-04-24 09:29:17','2026-04-24 09:29:17'),(14,30007,10001,0,1,0,0,0,'2026-04-24 09:29:47','2026-04-24 09:30:11'),(15,30007,10002,0,0,0,0,0,'2026-04-24 09:29:47','2026-04-24 09:29:47'),(16,30008,10001,0,1,0,0,0,'2026-04-24 09:32:03','2026-04-25 09:51:35'),(17,30008,10002,0,0,0,0,0,'2026-04-24 09:32:03','2026-04-24 09:32:03'),(18,30009,10001,0,2,0,0,0,'2026-04-24 09:34:05','2026-04-25 09:51:35'),(19,30009,10002,1,0,0,0,0,'2026-04-24 09:34:05','2026-04-24 09:34:05'),(20,30010,10001,0,2,1,1,0,'2026-04-24 09:36:40','2026-04-25 09:51:34'),(21,30010,10002,0,1,0,0,0,'2026-04-24 09:36:40','2026-04-24 09:36:40');
/*!40000 ALTER TABLE `im_conversation_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `im_file`
--

DROP TABLE IF EXISTS `im_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `im_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ–‡ä»¶ID',
  `owner_user_id` bigint NOT NULL COMMENT 'ä¸Šä¼ ç”¨æˆ·ID',
  `biz_type` tinyint NOT NULL COMMENT '1å¤´åƒ 2å›¾ç‰‡ 3è§†é¢‘ 4æ™®é€šæ–‡ä»¶',
  `storage_type` varchar(20) NOT NULL DEFAULT 'local' COMMENT 'local/minio/oss',
  `bucket_name` varchar(100) DEFAULT NULL COMMENT 'æ¡¶å',
  `object_key` varchar(255) NOT NULL COMMENT 'å¯¹è±¡è·¯å¾„',
  `file_name` varchar(255) NOT NULL COMMENT 'åŽŸå§‹æ–‡ä»¶å',
  `file_ext` varchar(20) DEFAULT NULL COMMENT 'æ–‡ä»¶åŽç¼€',
  `content_type` varchar(100) DEFAULT NULL COMMENT 'å†…å®¹ç±»åž‹',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT 'æ–‡ä»¶å¤§å°å­—èŠ‚',
  `md5` varchar(32) DEFAULT NULL COMMENT 'MD5',
  `url` varchar(255) DEFAULT NULL COMMENT 'è®¿é—®åœ°å€',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1æœ‰æ•ˆ 2åˆ é™¤',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_owner_user_id` (`owner_user_id`),
  KEY `idx_biz_type` (`biz_type`)
) ENGINE=InnoDB AUTO_INCREMENT=50002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æ–‡ä»¶èµ„æºè¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `im_file`
--

LOCK TABLES `im_file` WRITE;
/*!40000 ALTER TABLE `im_file` DISABLE KEYS */;
INSERT INTO `im_file` VALUES (50001,10001,2,'local',NULL,'upload/demo/welcome.png','welcome.png','png','image/png',102400,NULL,'/upload/demo/welcome.png',1,'2026-04-22 13:16:47','2026-04-22 14:02:34');
/*!40000 ALTER TABLE `im_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `im_friend`
--

DROP TABLE IF EXISTS `im_friend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `im_friend` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'å¥½å‹å…³ç³»ID',
  `user_id` bigint NOT NULL COMMENT 'ç”¨æˆ·ID',
  `friend_user_id` bigint NOT NULL COMMENT 'å¥½å‹ç”¨æˆ·ID',
  `remark` varchar(100) DEFAULT NULL COMMENT 'å¥½å‹å¤‡æ³¨',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1æ­£å¸¸ 2æ‹‰é»‘ 3åˆ é™¤',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_friend` (`user_id`,`friend_user_id`),
  KEY `idx_friend_user` (`friend_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='å¥½å‹å…³ç³»è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `im_friend`
--

LOCK TABLES `im_friend` WRITE;
/*!40000 ALTER TABLE `im_friend` DISABLE KEYS */;
INSERT INTO `im_friend` VALUES (1,10001,10002,'äº§å“åŒå­¦',1,'2026-04-22 13:16:47','2026-04-24 03:03:29'),(2,10002,10001,'ç ”å‘åŒå­¦',1,'2026-04-22 13:16:47','2026-04-24 03:03:29'),(3,10005,10006,NULL,1,'2026-04-22 14:51:41','2026-04-22 14:51:41'),(4,10006,10005,NULL,1,'2026-04-22 14:51:41','2026-04-22 14:51:41'),(5,10007,10008,NULL,1,'2026-04-23 02:16:32','2026-04-23 02:16:32'),(6,10008,10007,NULL,1,'2026-04-23 02:16:32','2026-04-23 02:16:32'),(7,10009,10010,NULL,1,'2026-04-23 13:12:37','2026-04-23 13:12:37'),(8,10010,10009,NULL,1,'2026-04-23 13:12:37','2026-04-23 13:12:37');
/*!40000 ALTER TABLE `im_friend` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `im_friend_request`
--

DROP TABLE IF EXISTS `im_friend_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `im_friend_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'å¥½å‹ç”³è¯·ID',
  `from_user_id` bigint NOT NULL COMMENT 'ç”³è¯·äººID',
  `to_user_id` bigint NOT NULL COMMENT 'æŽ¥æ”¶äººID',
  `apply_msg` varchar(255) DEFAULT NULL COMMENT 'ç”³è¯·ç•™è¨€',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0å¾…å¤„ç† 1åŒæ„ 2æ‹’ç» 3æ‹‰é»‘',
  `handled_by` bigint DEFAULT NULL COMMENT 'å¤„ç†äººID',
  `handled_at` datetime DEFAULT NULL COMMENT 'å¤„ç†æ—¶é—´',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_to_user_status` (`to_user_id`,`status`),
  KEY `idx_from_user` (`from_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='å¥½å‹ç”³è¯·è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `im_friend_request`
--

LOCK TABLES `im_friend_request` WRITE;
/*!40000 ALTER TABLE `im_friend_request` DISABLE KEYS */;
INSERT INTO `im_friend_request` VALUES (1,10001,10002,'ä½ å¥½ï¼Œæƒ³åŠ ä½ ä¸ºå¥½å‹',1,10002,'2026-04-22 14:02:34','2026-04-22 13:16:47','2026-04-22 14:02:34'),(2,10001,10003,'MVP联调测试',0,NULL,NULL,'2026-04-22 14:11:42','2026-04-22 14:11:42'),(3,10005,10006,'phase2 hello',1,10006,'2026-04-22 22:51:41','2026-04-22 14:51:41','2026-04-22 14:51:41'),(4,10007,10008,'chat phase3',1,10008,'2026-04-23 10:16:33','2026-04-23 02:16:32','2026-04-23 02:16:32'),(5,10009,10010,'offline sync hello',1,10010,'2026-04-23 21:12:37','2026-04-23 13:12:37','2026-04-23 13:12:37');
/*!40000 ALTER TABLE `im_friend_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `im_group`
--

DROP TABLE IF EXISTS `im_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `im_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç¾¤ç»„ID',
  `group_no` varchar(32) NOT NULL COMMENT 'ç¾¤ç¼–å·',
  `group_name` varchar(100) NOT NULL COMMENT 'ç¾¤åç§°',
  `owner_user_id` bigint NOT NULL COMMENT 'ç¾¤ä¸»ID',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT 'ç¾¤å¤´åƒ',
  `notice` text COMMENT 'ç¾¤å…¬å‘Š',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1æ­£å¸¸ 2è§£æ•£ 3ç¦ç”¨',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_no` (`group_no`),
  KEY `idx_owner_user` (`owner_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20007 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ç¾¤ç»„è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `im_group`
--

LOCK TABLES `im_group` WRITE;
/*!40000 ALTER TABLE `im_group` DISABLE KEYS */;
INSERT INTO `im_group` VALUES (20001,'G20001','Echo é¡¹ç›®è®¨è®ºç¾¤',10001,NULL,'æ¬¢è¿ŽåŠ å…¥ EchoIM é¡¹ç›®è®¨è®ºç¾¤ã€‚',1,'2026-04-22 13:16:47','2026-04-22 14:02:34'),(20002,'G1777022957584ca9656','第五阶段测试群',10001,NULL,NULL,1,'2026-04-24 09:29:17','2026-04-24 09:29:17'),(20003,'G17770229872786af89e','WS群聊测试',10001,NULL,NULL,1,'2026-04-24 09:29:47','2026-04-24 09:29:47'),(20004,'G1777023123969961d78','第五阶段群发验证',10001,NULL,NULL,1,'2026-04-24 09:32:03','2026-04-24 09:32:03'),(20005,'G17770232454565e0d9b','移除摘要验证',10001,NULL,NULL,1,'2026-04-24 09:34:05','2026-04-24 09:34:05'),(20006,'G1777023400607c20a9c','移除摘要验证2',10001,NULL,NULL,1,'2026-04-24 09:36:40','2026-04-24 09:36:40');
/*!40000 ALTER TABLE `im_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `im_group_member`
--

DROP TABLE IF EXISTS `im_group_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `im_group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç¾¤æˆå‘˜ID',
  `group_id` bigint NOT NULL COMMENT 'ç¾¤ç»„ID',
  `user_id` bigint NOT NULL COMMENT 'ç”¨æˆ·ID',
  `role` tinyint NOT NULL DEFAULT '2' COMMENT '1ç¾¤ä¸» 2æˆå‘˜ 3ç®¡ç†å‘˜',
  `nick_name` varchar(100) DEFAULT NULL COMMENT 'ç¾¤å†…æ˜µç§°',
  `join_source` tinyint NOT NULL DEFAULT '1' COMMENT '1åˆ›å»ºç¾¤ 2é‚€è¯·åŠ å…¥ 3ç”³è¯·åŠ å…¥',
  `join_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åŠ å…¥æ—¶é—´',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1æ­£å¸¸ 2é€€å‡º 3ç§»é™¤',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ç¾¤æˆå‘˜è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `im_group_member`
--

LOCK TABLES `im_group_member` WRITE;
/*!40000 ALTER TABLE `im_group_member` DISABLE KEYS */;
INSERT INTO `im_group_member` VALUES (1,20001,10001,1,'ç¾¤ä¸»-01',1,'2026-04-22 13:16:47',1,'2026-04-22 14:02:34'),(2,20001,10002,2,'æˆå‘˜-02',2,'2026-04-22 13:16:47',1,'2026-04-22 14:02:34'),(3,20001,10003,2,'æˆå‘˜-03',2,'2026-04-22 13:16:47',1,'2026-04-22 14:02:34'),(4,20002,10001,1,NULL,1,'2026-04-24 17:29:18',1,'2026-04-24 09:29:17'),(5,20002,10002,2,NULL,2,'2026-04-24 17:29:18',1,'2026-04-24 09:29:17'),(6,20003,10001,1,NULL,1,'2026-04-24 17:29:47',1,'2026-04-24 09:29:47'),(7,20003,10002,2,NULL,2,'2026-04-24 17:29:47',1,'2026-04-24 09:29:47'),(8,20004,10001,1,NULL,1,'2026-04-24 17:32:04',1,'2026-04-24 09:32:03'),(9,20004,10002,2,NULL,2,'2026-04-24 17:32:04',3,'2026-04-24 17:32:05'),(10,20005,10001,1,NULL,1,'2026-04-24 17:34:05',1,'2026-04-24 09:34:05'),(11,20005,10002,2,NULL,2,'2026-04-24 17:34:05',3,'2026-04-24 17:34:06'),(12,20006,10001,1,NULL,1,'2026-04-24 17:36:41',1,'2026-04-24 09:36:40'),(13,20006,10002,2,NULL,2,'2026-04-24 17:36:41',3,'2026-04-24 17:36:41');
/*!40000 ALTER TABLE `im_group_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `im_message`
--

DROP TABLE IF EXISTS `im_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `im_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ¶ˆæ¯ID',
  `conversation_id` bigint NOT NULL COMMENT 'ä¼šè¯ID',
  `conversation_type` tinyint NOT NULL COMMENT '1å•èŠ 2ç¾¤èŠ',
  `seq_no` bigint NOT NULL COMMENT 'ä¼šè¯å†…é€’å¢žåºå·',
  `client_msg_id` varchar(64) NOT NULL COMMENT 'å®¢æˆ·ç«¯æ¶ˆæ¯ID',
  `from_user_id` bigint NOT NULL COMMENT 'å‘é€äººID',
  `to_user_id` bigint DEFAULT NULL COMMENT 'æŽ¥æ”¶äººIDï¼Œå•èŠæ—¶ä½¿ç”¨',
  `group_id` bigint DEFAULT NULL COMMENT 'ç¾¤IDï¼Œç¾¤èŠæ—¶ä½¿ç”¨',
  `msg_type` tinyint NOT NULL COMMENT '1æ–‡æœ¬ 2è¡¨æƒ… 3å›¾ç‰‡ 4è§†é¢‘ 5æ–‡ä»¶ 6ç³»ç»Ÿæ¶ˆæ¯',
  `content` text COMMENT 'æ¶ˆæ¯å†…å®¹',
  `extra_json` json DEFAULT NULL COMMENT 'æ‰©å±•æ•°æ®',
  `file_id` bigint DEFAULT NULL COMMENT 'æ–‡ä»¶ID',
  `send_status` tinyint NOT NULL DEFAULT '1' COMMENT '1å‘é€æˆåŠŸ 2å‘é€å¤±è´¥ 3æ’¤å›ž',
  `sent_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'å‘é€æ—¶é—´',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_from_client_msg` (`from_user_id`,`client_msg_id`),
  UNIQUE KEY `uk_conversation_seq` (`conversation_id`,`seq_no`),
  KEY `idx_conversation_time` (`conversation_id`,`sent_at`),
  KEY `idx_to_user_id` (`to_user_id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB AUTO_INCREMENT=40031 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æ¶ˆæ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `im_message`
--

LOCK TABLES `im_message` WRITE;
/*!40000 ALTER TABLE `im_message` DISABLE KEYS */;
INSERT INTO `im_message` VALUES (40001,30001,1,1,'seed-cmsg-40001',10001,10002,NULL,1,'ä½ å¥½ï¼Œæ¬¢è¿Žæµ‹è¯• EchoIM',NULL,NULL,1,'2026-04-22 13:16:47','2026-04-22 13:16:47','2026-04-22 14:02:34'),(40002,30001,1,2,'seed-cmsg-40002',10002,10001,NULL,1,'ä»Šæ™šåŒæ­¥ä¸€ä¸‹æŽ¥å£è®¾è®¡',NULL,NULL,1,'2026-04-22 13:16:47','2026-04-22 13:16:47','2026-04-22 14:02:34'),(40003,30002,2,1,'seed-cmsg-40003',10001,NULL,20001,1,'å¤§å®¶æ™šä¸Šå¥½',NULL,NULL,1,'2026-04-22 13:16:47','2026-04-22 13:16:47','2026-04-22 14:02:34'),(40004,30003,1,1,'approve-3',10005,10006,NULL,6,'phase2 hello',NULL,NULL,1,'2026-04-22 22:51:41','2026-04-22 14:51:41','2026-04-22 14:51:41'),(40005,30004,1,1,'approve-4',10007,10008,NULL,6,'chat phase3',NULL,NULL,1,'2026-04-23 10:16:33','2026-04-23 02:16:32','2026-04-23 02:16:32'),(40006,30004,1,2,'phase3-1776910591606',10007,10008,NULL,1,'hello phase3 1776910591606',NULL,NULL,1,'2026-04-23 10:16:33','2026-04-23 02:16:32','2026-04-23 02:16:32'),(40007,30005,1,1,'approve-5',10009,10010,NULL,6,'offline sync hello',NULL,NULL,1,'2026-04-23 21:12:37','2026-04-23 13:12:37','2026-04-23 13:12:37'),(40008,30005,1,2,'offline-1776949955820-1',10009,10010,NULL,1,'offline sync msg 1',NULL,NULL,1,'2026-04-23 21:12:37','2026-04-23 13:12:37','2026-04-23 13:12:37'),(40009,30005,1,3,'offline-1776949955820-2',10009,10010,NULL,1,'offline sync msg 2',NULL,NULL,1,'2026-04-23 21:12:37','2026-04-23 13:12:37','2026-04-23 13:12:37'),(40010,30007,2,1,'group-test-1777022987631',10002,NULL,20003,1,'hello group',NULL,NULL,1,'2026-04-24 17:29:48','2026-04-24 09:29:47','2026-04-24 09:29:47'),(40011,30008,2,1,'group-ok-1777023124332',10002,NULL,20004,1,'group ok',NULL,NULL,1,'2026-04-24 17:32:04','2026-04-24 09:32:04','2026-04-24 09:32:04'),(40012,30009,2,1,'first-1777023245776',10001,NULL,20005,1,'before remove',NULL,NULL,1,'2026-04-24 17:34:06','2026-04-24 09:34:05','2026-04-24 09:34:05'),(40013,30009,2,2,'second-1777023246194',10001,NULL,20005,1,'after remove hidden',NULL,NULL,1,'2026-04-24 17:34:06','2026-04-24 09:34:06','2026-04-24 09:34:06'),(40014,30010,2,1,'first-1777023400949',10001,NULL,20006,1,'before remove',NULL,NULL,1,'2026-04-24 17:36:41','2026-04-24 09:36:40','2026-04-24 09:36:40'),(40015,30010,2,2,'second-1777023401381',10001,NULL,20006,1,'after remove hidden',NULL,NULL,1,'2026-04-24 17:36:41','2026-04-24 09:36:41','2026-04-24 09:36:41'),(40016,30001,1,3,'codex-1777108380746',10001,10002,NULL,1,'codex stage4 ws test',NULL,NULL,1,'2026-04-25 17:13:01','2026-04-25 09:13:01','2026-04-25 09:13:01'),(40017,30001,1,4,'local-1777109822710-c484d3',10001,10002,NULL,1,'pw-1777109816227-a',NULL,NULL,1,'2026-04-25 17:37:03','2026-04-25 09:37:02','2026-04-25 09:37:02'),(40018,30001,1,5,'local-1777110002809-2645ee',10001,10002,NULL,1,'pw-1777109987389-a',NULL,NULL,1,'2026-04-25 17:40:03','2026-04-25 09:40:02','2026-04-25 09:40:02'),(40019,30001,1,6,'node-msg-1',10001,10002,NULL,1,'node-1777110085348',NULL,NULL,1,'2026-04-25 17:41:25','2026-04-25 09:41:25','2026-04-25 09:41:25'),(40020,30001,1,7,'local-1777110160869-bd453d',10001,10002,NULL,1,'pw-1777110157190-a',NULL,NULL,1,'2026-04-25 17:42:41','2026-04-25 09:42:40','2026-04-25 09:42:40'),(40021,30001,1,8,'local-1777110258436-c44168',10001,10002,NULL,1,'pw-1777110253287-a',NULL,NULL,1,'2026-04-25 17:44:18','2026-04-25 09:44:18','2026-04-25 09:44:18'),(40022,30001,1,9,'local-1777110387571-f3f132',10001,10002,NULL,1,'pw-1777110373017-a',NULL,NULL,1,'2026-04-25 17:46:28','2026-04-25 09:46:27','2026-04-25 09:46:27'),(40023,30001,1,10,'local-1777110550917-363e74',10001,10002,NULL,1,'pw-1777110545313-a',NULL,NULL,1,'2026-04-25 17:49:11','2026-04-25 09:49:10','2026-04-25 09:49:10'),(40024,30001,1,11,'pw-ws-1777110552389',10001,10002,NULL,1,'ws-1777110552374',NULL,NULL,1,'2026-04-25 17:49:12','2026-04-25 09:49:12','2026-04-25 09:49:12'),(40025,30001,1,12,'local-1777110577650-e2b4b8',10001,10002,NULL,1,'pw-1777110574375-a',NULL,NULL,1,'2026-04-25 17:49:38','2026-04-25 09:49:37','2026-04-25 09:49:37'),(40026,30001,1,13,'local-1777110577921-de8e9c',10002,10001,NULL,1,'pw-1777110574375-b',NULL,NULL,1,'2026-04-25 17:49:38','2026-04-25 09:49:37','2026-04-25 09:49:37'),(40027,30001,1,14,'pw-ws-1777110578612',10001,10002,NULL,1,'ws-1777110578594',NULL,NULL,1,'2026-04-25 17:49:39','2026-04-25 09:49:38','2026-04-25 09:49:38'),(40028,30001,1,15,'local-1777110705903-08e8b4',10001,10002,NULL,1,'pw-1777110700051-a',NULL,NULL,1,'2026-04-25 17:51:46','2026-04-25 09:51:45','2026-04-25 09:51:45'),(40029,30001,1,16,'local-1777110706250-cd703b',10002,10001,NULL,1,'pw-1777110700051-b',NULL,NULL,1,'2026-04-25 17:51:46','2026-04-25 09:51:46','2026-04-25 09:51:46'),(40030,30001,1,17,'pw-ws-1777110706968',10001,10002,NULL,1,'ws-1777110706948',NULL,NULL,1,'2026-04-25 17:51:47','2026-04-25 09:51:46','2026-04-25 09:51:46');
/*!40000 ALTER TABLE `im_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `im_message_receipt`
--

DROP TABLE IF EXISTS `im_message_receipt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `im_message_receipt` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'å›žæ‰§ID',
  `message_id` bigint NOT NULL COMMENT 'æ¶ˆæ¯ID',
  `conversation_id` bigint NOT NULL COMMENT 'ä¼šè¯ID',
  `user_id` bigint NOT NULL COMMENT 'ç”¨æˆ·ID',
  `receipt_type` tinyint NOT NULL COMMENT '1é€è¾¾ 2å·²è¯»',
  `receipt_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'å›žæ‰§æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_user_receipt` (`message_id`,`user_id`,`receipt_type`),
  KEY `idx_conversation_user` (`conversation_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=157 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æ¶ˆæ¯å›žæ‰§è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `im_message_receipt`
--

LOCK TABLES `im_message_receipt` WRITE;
/*!40000 ALTER TABLE `im_message_receipt` DISABLE KEYS */;
INSERT INTO `im_message_receipt` VALUES (1,40001,30001,10002,1,'2026-04-22 14:02:34'),(2,40001,30001,10002,2,'2026-04-22 14:02:34'),(3,40002,30001,10001,1,'2026-04-22 14:02:34'),(4,40006,30004,10008,1,'2026-04-23 02:16:32'),(5,40005,30004,10008,2,'2026-04-23 02:16:32'),(6,40006,30004,10008,2,'2026-04-23 02:16:32'),(9,40007,30005,10010,2,'2026-04-23 13:12:37'),(10,40008,30005,10010,2,'2026-04-23 13:12:37'),(11,40009,30005,10010,2,'2026-04-23 13:12:37'),(12,40002,30001,10001,2,'2026-04-25 09:37:02'),(20,40019,30001,10002,1,'2026-04-25 09:41:26'),(37,40016,30001,10002,2,'2026-04-25 09:49:11'),(38,40017,30001,10002,2,'2026-04-25 09:49:11'),(39,40018,30001,10002,2,'2026-04-25 09:49:11'),(40,40019,30001,10002,2,'2026-04-25 09:49:11'),(41,40020,30001,10002,2,'2026-04-25 09:49:11'),(42,40021,30001,10002,2,'2026-04-25 09:49:11'),(43,40022,30001,10002,2,'2026-04-25 09:49:11'),(44,40023,30001,10002,2,'2026-04-25 09:49:11'),(55,40024,30001,10002,1,'2026-04-25 09:49:12'),(56,40024,30001,10002,2,'2026-04-25 09:49:12'),(61,40025,30001,10002,2,'2026-04-25 09:49:37'),(65,40026,30001,10001,2,'2026-04-25 09:49:37'),(67,40027,30001,10002,1,'2026-04-25 09:49:38'),(68,40027,30001,10002,2,'2026-04-25 09:49:38'),(87,40028,30001,10002,2,'2026-04-25 09:51:46'),(91,40029,30001,10001,2,'2026-04-25 09:51:46'),(93,40030,30001,10002,1,'2026-04-25 09:51:47'),(94,40030,30001,10002,2,'2026-04-25 09:51:47');
/*!40000 ALTER TABLE `im_message_receipt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `im_user`
--

DROP TABLE IF EXISTS `im_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `im_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç”¨æˆ·ID',
  `user_no` varchar(32) NOT NULL COMMENT 'ç”¨æˆ·ç¼–å·æˆ–ç™»å½•å·',
  `username` varchar(50) NOT NULL COMMENT 'ç”¨æˆ·å',
  `password_hash` varchar(255) NOT NULL COMMENT 'å¯†ç å“ˆå¸Œ',
  `nickname` varchar(50) NOT NULL COMMENT 'æ˜µç§°',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT 'å¤´åƒ',
  `gender` tinyint NOT NULL DEFAULT '0' COMMENT '0æœªçŸ¥ 1ç”· 2å¥³',
  `phone` varchar(20) DEFAULT NULL COMMENT 'æ‰‹æœºå·',
  `email` varchar(100) DEFAULT NULL COMMENT 'é‚®ç®±',
  `signature` varchar(255) DEFAULT NULL COMMENT 'ä¸ªæ€§ç­¾å',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1æ­£å¸¸ 2ç¦ç”¨ 3æ³¨é”€',
  `last_login_at` datetime DEFAULT NULL COMMENT 'æœ€åŽç™»å½•æ—¶é—´',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`user_no`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=10011 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ç”¨æˆ·è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `im_user`
--

LOCK TABLES `im_user` WRITE;
/*!40000 ALTER TABLE `im_user` DISABLE KEYS */;
INSERT INTO `im_user` VALUES (10001,'E10001','echo_demo_01','$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y','Echo���Ʒ01',NULL,1,'13800000001','echo01@example.com','欢�}来�ư EchoIM',1,'2026-04-25 18:54:20','2026-04-22 13:16:47','2026-04-22 14:07:29'),(10002,'E10002','echo_demo_02','$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y','Echoç”¨æˆ·02',NULL,2,'13800000002','echo02@example.com','åœ¨çº¿æ²Ÿé€šæ›´é«˜æ•ˆ',1,'2026-04-25 17:51:47','2026-04-22 13:16:47','2026-04-22 14:07:29'),(10003,'E10003','echo_demo_03','$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y','Echoç”¨æˆ·03',NULL,1,'13800000003','echo03@example.com','è¿™æ˜¯ç¬¬ä¸‰ä¸ªæ¼”ç¤ºè´¦å·',1,'2026-04-22 13:16:47','2026-04-22 13:16:47','2026-04-22 14:07:29'),(10004,'E10004','echo_new_user_01','$2a$10$HgXMfSkJGg1dM3RDNdPerOY3uRbkn.oF71oYYKVWlC2PZfH5CmMvm','新用户01',NULL,0,NULL,NULL,NULL,1,NULL,'2026-04-22 14:12:41','2026-04-22 14:12:41'),(10005,'E10005','phase2_1776869500_a','$2a$10$tiPXiCQeK.ucBhZLivPOfOSw/opc6kywL/ia0yZk52sMckRDKxgPu','Phase2 Updated A','https://example.com/avatar-a.png',1,NULL,NULL,'phase2-signature',1,'2026-04-22 23:10:24','2026-04-22 14:51:40','2026-04-22 14:51:40'),(10006,'E10006','phase2_1776869500_b','$2a$10$37/t/J.fYAnU24L9CUcEuOTxWgpg6EDaypmfYyZCVnV37S5.5UBtm','Phase2B9500',NULL,0,NULL,NULL,NULL,1,'2026-04-22 23:12:58','2026-04-22 14:51:40','2026-04-22 14:51:40'),(10007,'E10007','chat_1776910591606_a','$2a$10$27BvfRyFQHsCA9Aan6iK5efOWmHO/T/9qu23ikKdRk87n9eruruPO','ChatA1606',NULL,0,NULL,NULL,NULL,1,'2026-04-23 10:17:32','2026-04-23 02:16:32','2026-04-23 02:16:32'),(10008,'E10008','chat_1776910591606_b','$2a$10$EjGqBwtPRa1692Snak78OOVn3cLE9mAMtowUJnozn9dodqHGEd3Mm','ChatB1606',NULL,0,NULL,NULL,NULL,1,'2026-04-23 10:16:33','2026-04-23 02:16:32','2026-04-23 02:16:32'),(10009,'E10009','offline_1776949955820_a','$2a$10$Jmr6D7CoOQ.rVRbovifYf.RMvnFAKm.H2oN43z6g02.sWfsBifymG','OfflineA5820',NULL,0,NULL,NULL,NULL,1,'2026-04-23 21:12:37','2026-04-23 13:12:36','2026-04-23 13:12:36'),(10010,'E10010','offline_1776949955820_b','$2a$10$Y5Tqavj3X9SYdOt8xAiYHuNRwOuYSCyLW/YDBICssEp1ax73ZfAje','OfflineB5820',NULL,0,NULL,NULL,NULL,1,'2026-04-23 21:13:58','2026-04-23 13:12:36','2026-04-23 13:12:36');
/*!40000 ALTER TABLE `im_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_admin_user`
--

DROP TABLE IF EXISTS `sys_admin_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç®¡ç†å‘˜ID',
  `username` varchar(50) NOT NULL COMMENT 'ç®¡ç†å‘˜è´¦å·',
  `password_hash` varchar(255) NOT NULL COMMENT 'å¯†ç å“ˆå¸Œ',
  `nickname` varchar(50) NOT NULL COMMENT 'ç®¡ç†å‘˜æ˜µç§°',
  `role_code` varchar(50) NOT NULL COMMENT 'è§’è‰²ç¼–ç ',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1æ­£å¸¸ 2ç¦ç”¨',
  `last_login_at` datetime DEFAULT NULL COMMENT 'æœ€åŽç™»å½•æ—¶é—´',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ç®¡ç†å‘˜è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_admin_user`
--

LOCK TABLES `sys_admin_user` WRITE;
/*!40000 ALTER TABLE `sys_admin_user` DISABLE KEYS */;
INSERT INTO `sys_admin_user` VALUES (1,'admin','$2y$10$EbnxVPdt9jN1hV5IYzGMQe1Cd0i8Nta611/Yj.Rfb4YgIgH90XS1y','ç³»ç»Ÿç®¡ç†å‘˜','super_admin',1,NULL,'2026-04-22 13:16:47','2026-04-22 14:07:29');
/*!40000 ALTER TABLE `sys_admin_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_beauty_no`
--

DROP TABLE IF EXISTS `sys_beauty_no`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_beauty_no` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'é“å·ID',
  `beauty_no` varchar(32) NOT NULL COMMENT 'é“å·',
  `bind_user_id` bigint DEFAULT NULL COMMENT 'ç»‘å®šç”¨æˆ·ID',
  `level_type` tinyint NOT NULL DEFAULT '1' COMMENT '1æ™®é€š 2ç¨€æœ‰ 3é«˜ä»·å€¼',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1æœªä½¿ç”¨ 2å·²ç»‘å®š 3åœç”¨',
  `remark` varchar(255) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_beauty_no` (`beauty_no`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='é“å·è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_beauty_no`
--

LOCK TABLES `sys_beauty_no` WRITE;
/*!40000 ALTER TABLE `sys_beauty_no` DISABLE KEYS */;
INSERT INTO `sys_beauty_no` VALUES (1,'10000',NULL,2,1,'æ¼”ç¤ºç¨€æœ‰é“å·','2026-04-22 13:16:47','2026-04-22 14:02:34'),(2,'88888',NULL,3,1,'æ¼”ç¤ºé«˜ä»·å€¼é“å·','2026-04-22 13:16:47','2026-04-22 14:02:34');
/*!40000 ALTER TABLE `sys_beauty_no` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'é…ç½®ID',
  `config_key` varchar(100) NOT NULL COMMENT 'é…ç½®é”®',
  `config_value` text NOT NULL COMMENT 'é…ç½®å€¼',
  `config_name` varchar(100) NOT NULL COMMENT 'é…ç½®åç§°',
  `remark` varchar(255) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1å¯ç”¨ 0åœç”¨',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ç³»ç»Ÿé…ç½®è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` VALUES (1,'file.max-size-mb','50','æ–‡ä»¶ä¸Šä¼ å¤§å°é™åˆ¶','ä¸Šä¼ æ–‡ä»¶æœ€å¤§ 50MB',1,'2026-04-22 13:16:47','2026-04-22 14:02:34'),(2,'message.recall-seconds','120','æ¶ˆæ¯æ’¤å›žæ—¶é—´é™åˆ¶','å‘é€åŽ 120 ç§’å†…å¯æ’¤å›ž',1,'2026-04-22 13:16:47','2026-04-22 14:02:34'),(3,'register.enabled','true','æ˜¯å¦å…è®¸æ³¨å†Œ','æŽ§åˆ¶å‰å°æ³¨å†Œå¼€å…³',1,'2026-04-22 13:16:47','2026-04-22 14:02:34');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_operation_log`
--

DROP TABLE IF EXISTS `sys_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ—¥å¿—ID',
  `admin_user_id` bigint NOT NULL COMMENT 'ç®¡ç†å‘˜ID',
  `module_name` varchar(50) NOT NULL COMMENT 'æ¨¡å—å',
  `action_name` varchar(50) NOT NULL COMMENT 'æ“ä½œå',
  `target_type` varchar(50) DEFAULT NULL COMMENT 'ç›®æ ‡ç±»åž‹',
  `target_id` bigint DEFAULT NULL COMMENT 'ç›®æ ‡ID',
  `request_ip` varchar(64) DEFAULT NULL COMMENT 'è¯·æ±‚IP',
  `content_json` json DEFAULT NULL COMMENT 'æ—¥å¿—è¯¦æƒ…',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_admin_user_id` (`admin_user_id`),
  KEY `idx_module_name` (`module_name`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æ“ä½œæ—¥å¿—è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_operation_log`
--

LOCK TABLES `sys_operation_log` WRITE;
/*!40000 ALTER TABLE `sys_operation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_operation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_version`
--

DROP TABLE IF EXISTS `sys_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç‰ˆæœ¬ID',
  `version_code` varchar(50) NOT NULL COMMENT 'ç‰ˆæœ¬å·',
  `version_name` varchar(100) NOT NULL COMMENT 'ç‰ˆæœ¬åç§°',
  `platform` varchar(20) NOT NULL DEFAULT 'web' COMMENT 'å¹³å°',
  `release_note` text COMMENT 'æ›´æ–°è¯´æ˜Ž',
  `force_update` tinyint NOT NULL DEFAULT '0' COMMENT '0å¦ 1æ˜¯',
  `gray_percent` int NOT NULL DEFAULT '100' COMMENT 'ç°åº¦ç™¾åˆ†æ¯”',
  `publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '0è‰ç¨¿ 1å·²å‘å¸ƒ',
  `published_at` datetime DEFAULT NULL COMMENT 'å‘å¸ƒæ—¶é—´',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_code` (`version_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ç‰ˆæœ¬ä¿¡æ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_version`
--

LOCK TABLES `sys_version` WRITE;
/*!40000 ALTER TABLE `sys_version` DISABLE KEYS */;
INSERT INTO `sys_version` VALUES (1,'v0.1.0','EchoIM MVP','web','åˆå§‹æ¼”ç¤ºç‰ˆæœ¬ï¼Œæ”¯æŒæ³¨å†Œç™»å½•ã€è”ç³»äººã€å•èŠå’Œç¾¤èŠæ–‡å­—æ¶ˆæ¯ã€‚',0,100,1,'2026-04-22 14:02:34','2026-04-22 13:16:47','2026-04-22 14:02:34');
/*!40000 ALTER TABLE `sys_version` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-25 11:01:09
