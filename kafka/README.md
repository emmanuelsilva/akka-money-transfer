# Kafka deployment

```shell
kubectl apply -n kafka -f 01-kafka-namespace.yaml
kubectl apply -n kafka -f 02-zookeeper/01-zookeeper-service.yaml
kubectl apply -n kafka -f 02-zookeeper/02-zookeeper-deployment.yaml
kubectl apply -n kafka -f 03-kafka-deployment.yaml
```
