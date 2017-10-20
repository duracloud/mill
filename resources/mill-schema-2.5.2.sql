--
-- Server version	5.6.34-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `audit_log_item`
--

DROP TABLE IF EXISTS `audit_log_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `audit_log_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `modified` datetime(3) NOT NULL,
  `account` varchar(255) NOT NULL,
  `action` varchar(255) NOT NULL,
  `content_id` varchar(1024) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `content_md5` varchar(255) DEFAULT NULL,
  `content_properties` varchar(2048) DEFAULT NULL,
  `content_size` varchar(255) DEFAULT NULL,
  `mimetype` varchar(255) DEFAULT NULL,
  `source_content_id` varchar(255) DEFAULT NULL,
  `source_space_id` varchar(255) DEFAULT NULL,
  `space_acls` varchar(2048) DEFAULT NULL,
  `space_id` varchar(255) NOT NULL,
  `store_id` varchar(255) NOT NULL,
  `timestamp` bigint(20) NOT NULL,
  `unique_key` char(32) NOT NULL,
  `username` varchar(255) NOT NULL,
  `written` bit(1) NOT NULL,
  `version` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unique_key` (`unique_key`),
  KEY `idx_timestamp_written` (`written`,`timestamp`),
  KEY `idx_content_id` (`account`,`store_id`,`space_id`,`content_id`(255))
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bit_log_item`
--

DROP TABLE IF EXISTS `bit_log_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bit_log_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `modified` datetime(3) NOT NULL,
  `account` varchar(255) NOT NULL,
  `content_checksum` varchar(255) DEFAULT NULL,
  `content_id` varchar(1024) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `details` varchar(1024) DEFAULT NULL,
  `manifest_checksum` varchar(255) DEFAULT NULL,
  `result` varchar(255) DEFAULT NULL,
  `space_id` varchar(255) NOT NULL,
  `storage_provider_checksum` varchar(255) DEFAULT NULL,
  `storage_provider_type` varchar(255) DEFAULT NULL,
  `store_id` varchar(255) NOT NULL,
  `version` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_content_id` (`account`,`store_id`,`space_id`,`content_id`(150)),
  KEY `idx_space_id` (`account`,`store_id`,`space_id`),
  KEY `idx_result_id` (`account`,`store_id`,`space_id`,`result`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bit_report`
--

DROP TABLE IF EXISTS `bit_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bit_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `modified` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `account` varchar(255) NOT NULL,
  `completion_date` datetime NOT NULL,
  `display` bit(1) NOT NULL,
  `report_content_id` varchar(1024) NOT NULL,
  `report_space_id` varchar(255) NOT NULL,
  `result` varchar(255) DEFAULT NULL,
  `space_id` varchar(255) NOT NULL,
  `store_id` varchar(255) NOT NULL,
  `version` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `manifest_item`
--

DROP TABLE IF EXISTS `manifest_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `manifest_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `modified` datetime(3) NOT NULL,
  `account` varchar(255) NOT NULL,
  `content_checksum` varchar(255) NOT NULL,
  `content_id` varchar(1024) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `content_mimetype` varchar(255) NOT NULL,
  `content_size` varchar(255) NOT NULL,
  `deleted` bit(1) NOT NULL,
  `missing_from_storage_provider` bit(1) NOT NULL,
  `space_id` varchar(255) NOT NULL,
  `store_id` varchar(255) NOT NULL,
  `unique_key` char(32) NOT NULL,
  `version` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_key` (`unique_key`),
  KEY `idx_account_id` (`deleted`,`account`),
  KEY `idx_space_id_2` (`account`,`store_id`,`space_id`,`deleted`),
  KEY `idx_content_id_2` (`account`,`store_id`,`space_id`,`content_id`(255))
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `space_stats`
--

DROP TABLE IF EXISTS `space_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `space_stats` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `modified` datetime NOT NULL,
  `account_id` varchar(65) NOT NULL,
  `space_id` varchar(65) NOT NULL,
  `store_id` varchar(10) NOT NULL,
  `byte_count` bigint(20) NOT NULL DEFAULT '0',
  `object_count` bigint(20) NOT NULL DEFAULT '0',
  `version` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_space_stat_unique` (`account_id`,`store_id`,`space_id`,`modified`),
  KEY `idx_space_stat_space` (`account_id`,`store_id`,`space_id`),
  KEY `idx_space_stat_store` (`account_id`,`store_id`),
  KEY `idx_space_stat_account_id` (`account_id`),
  KEY `idx_modified` (`modified`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2017-09-05 21:30:39
