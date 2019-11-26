#! /bin/bash

set -e

docker build -t ${LOCAL_DOCKER_REGISTRY}/rabbitmq-operator:latest -f rabbitmq-operator/Dockerfile .
docker push ${LOCAL_DOCKER_REGISTRY}/rabbitmq-operator:latest