-- MySQL dump 10.13  Distrib 5.5.38, for osx10.6 (i386)
--
-- Host: testdb.cg0c41jeyzcl.us-east-1.rds.amazonaws.com    Database: mill
-- ------------------------------------------------------
-- Server version	5.5.40

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
  `modified` datetime NOT NULL,
  `account` varchar(255) NOT NULL,
  `action` varchar(255) NOT NULL,
  `content_id` varchar(1024) DEFAULT NULL,
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_f6pwvtcuobqi6eq5gg9w6886v` (`unique_key`),
  KEY `idx_content_id` (`account`,`store_id`,`space_id`,`content_id`(150))
) ENGINE=InnoDB AUTO_INCREMENT=957036 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bit_log_item`
--

DROP TABLE IF EXISTS `bit_log_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bit_log_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `modified` datetime NOT NULL,
  `account` varchar(255) NOT NULL,
  `content_checksum` varchar(255) DEFAULT NULL,
  `content_id` varchar(1024) NOT NULL,
  `details` varchar(1024) DEFAULT NULL,
  `manifest_checksum` varchar(255) DEFAULT NULL,
  `result` varchar(255) DEFAULT NULL,
  `space_id` varchar(255) NOT NULL,
  `storage_provider_checksum` varchar(255) DEFAULT NULL,
  `storage_provider_type` varchar(255) DEFAULT NULL,
  `store_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_content_id` (`account`,`store_id`,`space_id`,`content_id`(150)),
  KEY `idx_space_id` (`account`,`store_id`,`space_id`),
  KEY `idx_result_id` (`account`,`store_id`,`space_id`,`result`)
) ENGINE=InnoDB AUTO_INCREMENT=793716 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bit_report`
--

DROP TABLE IF EXISTS `bit_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bit_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `modified` datetime NOT NULL,
  `account` varchar(255) NOT NULL,
  `completion_date` datetime NOT NULL,
  `display` bit(1) NOT NULL,
  `report_content_id` varchar(1024) NOT NULL,
  `report_space_id` varchar(255) NOT NULL,
  `result` varchar(255) DEFAULT NULL,
  `space_id` varchar(255) NOT NULL,
  `store_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=80 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `manifest_item`
--

DROP TABLE IF EXISTS `manifest_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `manifest_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `modified` datetime NOT NULL,
  `account` varchar(255) NOT NULL,
  `content_checksum` varchar(255)  NOT NULL,
  `content_id` varchar(1024)  NOT NULL,
  `content_mimetype` varchar(255)  NOT NULL,
  `content_size` varchar(255) NOT NULL,
  `deleted` bit(1) NOT NULL,
  `missing_from_storage_provider` bit(1) NOT NULL,
  `space_id` varchar(255)  NOT NULL,
  `store_id` varchar(255)  NOT NULL,
  `unique_key` char(32)  NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_o01i0408x5vgq8qwy4mfsgcs4` (`unique_key`),
  KEY `idx_content_id` (`account`(191),`store_id`(191),`space_id`(191),`content_id`(150)),
  KEY `idx_space_id` (`account`(191),`store_id`(191),`space_id`(191))
) ENGINE=InnoDB AUTO_INCREMENT=2428720 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-03-17 13:16:33
