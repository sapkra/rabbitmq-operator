#!/usr/bin/env bash

sed -i s/indeedoss/$LOCAL_DOCKER_REGISTRY/g deploy/operator.yaml || sed -i "" s/indeedoss/$LOCAL_DOCKER_REGISTRY/g deploy/operator.yaml
kubectl apply -k ./deploy
