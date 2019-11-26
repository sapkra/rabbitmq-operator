#!/usr/bin/env bash

# This assumes that there's only one operator instance
POD=`kubectl get pods --namespace rabbitmq-operator --no-headers | grep rabbitmq-operator | cut -d ' ' -f 1`
echo "Tailing pod $POD"
kubectl logs -f --namespace rabbitmq-operator $POD
