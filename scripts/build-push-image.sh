#! /bin/bash

set -e

docker build -t ${LOCAL_DOCKER_REGISTRY}/rabbitmq-operator:latest ./pkg
docker push ${LOCAL_DOCKER_REGISTRY}/rabbitmq-operator:latest