#!/bin/bash

# ensure the environment is available in cron jobs
printenv | grep -v "no_proxy" >> /etc/environment

# ensure the scripts are executable
chmod u+x /opt/app/*.sh

# set up cron jobs if this is a sentinel node
if [[ ${NODE_TYPE} == "sentinel" ]]; then
  ln -s /opt/app/sentinel-crontab /etc/cron/cron.d/sentinel
fi

# update the hostname in etc hosts
sh -xc "echo ${HOST_NAME} > /etc/hostname; hostname -F /etc/hostname"
sh -xc "sed -i -e '/^127.0.1.1/d' /etc/hosts; echo 127.0.1.1 ${HOST_NAME}.${DOMAIN} ${HOST_NAME} >> /etc/hosts"

# configure sumo
cp $MILL_HOME/sumo.conf /opt/SumoCollector/config/user.properties

# start sumo
service collector start

# start the node specific startup script

nohup /opt/app/${NODE_TYPE}.sh &>/dev/null &
