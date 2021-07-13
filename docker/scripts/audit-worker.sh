#!/bin/bash
echo "running workman";
# execute workman
java -Dlog.level=$LOG_LEVEL -jar /opt/app/workman-${MILL_VERSION}.jar -c "${CONFIG_FILE_PATH}" -q "queue.name.audit,queue.name.storagestats,queue.name.dup-high-priority,queue.name.dup-low-priority";
