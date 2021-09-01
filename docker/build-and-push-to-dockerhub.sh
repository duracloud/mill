#!/bin/bash

# exit when any command fails:
set -e

# authenticate with docker hub and then push images:
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker buildx create --use
platforms=linux/arm64,linux/amd64
org=dbernstein
# build and push images
for docker_tag in "${@:2}"
do
    echo "Building and pushing $docker_tag ..."
    if [ "latest" == $docker_tag ]
    then
        docker buildx build --platform ${platforms} --push  -t ${org}/mill .
	
    else
 	docker buildx build --platform ${platforms} --push  -t ${org}/mill:$docker_tag .
    fi

    echo "Build and push complete for $docker_tag"
done
