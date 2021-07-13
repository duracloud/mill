FROM ubuntu:impish

RUN apt update && apt install openjdk-11-jre-headless wget -y
RUN mkdir /opt/app

RUN wget https://collectors.sumologic.com/rest/download/deb/64 -O SumoCollector.deb && chmod +x SumoCollector.deb
RUN dpkg -i SumoCollector.deb
RUN rm SumoCollector.deb
# the version of the mill
ARG VERSION
ADD workman/target/workman-${VERSION}.jar /opt/app/
ADD manifest-builder/target/manifest-builder-${VERSION}.jar /opt/app/
ADD manifest-cleaner/target/manifest-cleaner-${VERSION}.jar /opt/app/
ADD auditlog-generator/target/auditlog-generator-${VERSION}.jar /opt/app/
ADD loopingduptaskproducer/target/loopingduptaskproducer-${VERSION}.jar /opt/app/
ADD looping-storagestats-taskproducer/target/looping-storagestats-taskproducer-${VERSION}.jar /opt/app/
ADD storage-reporter/target/storage-reporter-${VERSION}.jar /opt/app/
ADD loopingbittaskproducer/target/loopingbittaskproducer-${VERSION}.jar /opt/app/
ADD docker/scripts/setup.sh /opt/app/
ADD docker/scripts/audit-worker.sh /opt/app/
ADD docker/scripts/bit-worker.sh /opt/app/
ADD docker/scripts/dup-worker.sh /opt/app/
ADD docker/scripts/dup-producer.sh /opt/app/
ADD docker/scripts/storage-stats-producer.sh /opt/app/
ADD docker/scripts/bit-report-worker.sh /opt/app/
ADD docker/scripts/sentinel.sh /opt/app/
ADD docker/resources/sumo-sources.json /opt/app/

ENV LOG_LEVEL=DEBUG
ENV MILL_VERSION=${VERSION}
ENV AWS_REGION=us-east-1
ENV INSTANCE_PREFIX=dev
ENV INSTANCE_DOMAIN=my.domain
ENV INSTANCE_ID=instance_id
ENV CONFIG_FILE_PATH="/mill-home/mill-config.properties"
ENV NODE_TYPE=audit-worker
ENV MILL_HOME=/mill-home
CMD [ "sh", "-c", "echo node type = ${NODE_TYPE}; . /opt/app/setup.sh && . /opt/app/${NODE_TYPE}.sh" ] 
