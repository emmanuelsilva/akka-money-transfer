# Mysql deployment

```shell
kubectl apply -f 01-mysql-namespace.yaml
kubectl apply -n mysql -f 02-mysql-persistent-volume.yaml
kubectl apply -n mysql -f 03-mysql-deployment.yaml
```
