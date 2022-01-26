#!/bin/bash
echo "running dup producer";
# execute
java -Xmx512m -Djdk.xml.entityExpansionLimit=0  -Dduracloud.home=$MILL_HOME -Dlog.level=$LOG_LEVEL -jar /opt/app/loopingduptaskproducer-${MILL_VERSION}.jar -c "${CONFIG_FILE_PATH}" -w /efs/dup-producer
