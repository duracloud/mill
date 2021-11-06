#!/bin/bash

            include duracloud_mill::auditlog_generator
            include duracloud_mill::manifest_cleaner
            include duracloud_mill::storage_stats_producer
            include duracloud_mill::bit_producer
            include duracloud_mill::storage_reporter
            include duracloud_mill::efs_cleanup
            include duracloud_mill::dup_producer

