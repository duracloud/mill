#!/bin/bash
echo "running storage-stats-producer";
# execute
java -Xmx512m -Djdk.xml.entityExpansionLimit=0  -Dduracloud.home=$MILL_HOME -Dlog.level=$LOG_LEVEL -jar /opt/app/looping-storagestats-taskproducer-${MILL_VERSION}.jar -c "${CONFIG_FILE_PATH}" -w /efs/storage-stats-producer
