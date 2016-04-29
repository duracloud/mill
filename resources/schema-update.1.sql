CREATE TABLE IF NOT EXISTS `space_stats` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `modified` datetime NOT NULL,
  `account_id` varchar(65) NOT NULL,
  `space_id` varchar(65) NOT NULL,
  `store_id` varchar(10) NOT NULL,
  `byte_count` bigint(20) NOT NULL DEFAULT '0',
  `object_count` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_space_stat_unique` (`account_id`,`store_id`,`space_id`,`modified`),
  KEY `idx_space_stat_space` (`account_id`,`store_id`,`space_id`),
  KEY `idx_space_stat_store` (`account_id`,`store_id`),
  KEY `idx_space_stat_account_id` (`account_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17819 DEFAULT CHARSET=utf8;
