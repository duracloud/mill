#!/bin/bash
log_file=/efs/efs-cleanup.log
echo "$(date) starting cleanup..." >>  ${log_file}
#delete content-item*tmp files or storage stats state files older than 6 hours
find /efs  -type f \( -name "content-item*tmp" -o -name "storagestats-producer*"  \) -mmin +360 -delete -print >> ${log_file}

#delete empty directories modified more than 6 hours ago.
find /efs -mindepth 1 -type d -empty -mmin +360 -print -delete  >> ${log_file}


echo "$(date) cleanup complete. " >>  ${log_file}
