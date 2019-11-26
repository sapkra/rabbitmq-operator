#!/usr/bin/env bash

sed -i s/indeedoss/$LOCAL_DOCKER_REGISTRY/g examples/rabbitmq_operator.yaml
kubectl apply -f ./examples/rabbitmq_operator.yaml
