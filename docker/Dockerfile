FROM ubuntu:22.04

RUN apt-get update && apt-get install openjdk-11-jre-headless wget cron -y
RUN mkdir /opt/app

RUN wget -q https://collectors.sumologic.com/rest/download/deb/64 -O SumoCollector.deb && \ 
   chmod +x SumoCollector.deb && \
   dpkg -i SumoCollector.deb && \
   rm SumoCollector.deb && \
   rm -rf /var/lib/apt/lists/*

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
ADD docker/scripts/* /opt/app/
ADD docker/resources/sumo_sources.json /opt/

ENV LOG_LEVEL=DEBUG
ENV MILL_VERSION=${VERSION}
ENV MAX_WORKER_THREADS=20
ENV AWS_REGION=us-east-1
ENV INSTANCE_PREFIX=dev
ENV INSTANCE_DOMAIN=my.domain
ENV INSTANCE_ID=instance_id
ENV CONFIG_FILE_PATH="/mill-home/mill-config.properties"
ENV NODE_TYPE=audit-worker
ENV MILL_HOME=/mill-home
CMD [ "sh", "-c", "echo node type = ${NODE_TYPE}; . /opt/app/setup.sh && . /opt/app/${NODE_TYPE}.sh && sleep infinity" ] 
