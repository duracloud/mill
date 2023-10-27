FROM ghcr.io/phusion/baseimage:jammy-1.0.1

RUN apt-get update && apt-get install openjdk-17-jre-headless wget vim telnet -y
RUN mkdir -p /opt/app

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
ADD docker/resources/sumo_sources.json /opt/
ADD docker/scripts/* /opt/app/
ADD docker/startup/* /etc/my_init.d/

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

CMD ["/sbin/my_init"]
