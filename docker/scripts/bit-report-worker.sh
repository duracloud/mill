#!/bin/bash
echo "running workman";
# execute workman
java -Dlog.level=$LOG_LEVEL -Dduracloud.home=$MILL_HOME -jar /opt/app/workman-${MILL_VERSION}.jar -c "${CONFIG_FILE_PATH}" -q "queue.name.bit-report" 
