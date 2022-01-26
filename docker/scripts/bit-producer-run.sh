#!/bin/bash
echo "running bit producer";
# execute
java -Xmx512m -Djdk.xml.entityExpansionLimit=0  -Dduracloud.home=$MILL_HOME -Dlog.level=$LOG_LEVEL -jar /opt/app/loopingbittaskproducer-${MILL_VERSION}.jar -c "${CONFIG_FILE_PATH}" -w /efs/bit-producer
