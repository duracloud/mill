#!/bin/bash
echo "running storage-reporter";
# execute
java -Xmx512m -Djdk.xml.entityExpansionLimit=0  -Dduracloud.home=$MILL_HOME -Dlog.level=$LOG_LEVEL -jar /opt/app/storage-reporter-${MILL_VERSION}.jar -c "${CONFIG_FILE_PATH}" 
