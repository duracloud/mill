#!/bin/bash

echo "Enabling cron..."
service cron restart 
ln -s /opt/app/sentinel-crontab /etc/cron.d/sentinel-crontab
crontab /etc/cron.d/sentinel-crontab
echo "cron enabled."
