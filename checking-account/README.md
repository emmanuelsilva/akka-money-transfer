# Checking account service

Service responsible to manage checking accounts

## Deployment

1 - Generate docker image
```shell
mvn spring-boot:build-image
```

2 - Push docker image

```shell
docker tag <image-id> emmanuelsilva/checking-account
docker push emmanuelsilva/checking-account
```

```shell
kubectl apply -f kubernetes/01-namespace.yaml
kubectl apply -n checking-account -f kubernetes/02-deployment.yaml
kubectl apply -n checking-account -f kubernetes/03-service.yaml
kubectl apply -n checking-account -f kubernetes/04-ingress.yaml
```
