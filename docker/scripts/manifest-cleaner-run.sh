#!/bin/bash
echo "running manifest-cleaner";
# execute
java -Xmx512m -Djdk.xml.entityExpansionLimit=0  -Dduracloud.home=$MILL_HOME -Dlog.level=$LOG_LEVEL -jar /opt/app/manifest-cleaner-${MILL_VERSION}.jar -c "${CONFIG_FILE_PATH}" -w /efs/manifest-cleaner
