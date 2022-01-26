#!/bin/bash
echo "running auditlog-generator";
# execute
java -Xmx512m -Djdk.xml.entityExpansionLimit=0  -Dduracloud.home=$MILL_HOME -Dlog.level=$LOG_LEVEL -jar /opt/app/auditlog-generator-${MILL_VERSION}.jar -c "${CONFIG_FILE_PATH}" -w /efs/auditlog-generator
