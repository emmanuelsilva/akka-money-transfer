# Ledger
* * *

Every account is an Akka actor model to deal with concurrency issues.

The actors are distributed across the cluster nodes using the cluster sharding approach.

All changes are stored as an event using the event sourcing approach.

## Setup
* * *

The project setup is based on the SBT tool.

- Test: `sbt test`

## Kubernetes deploy 
* * *

### Build docker image:

```shell
sbt docker:publishLocal
```

### Create Kubernetes resources:

Start minikube if it's not started yet:

```shell
minikube start --vm=true
eval $(minikube docker-env)
minikube addons enable ingress
```

*The argument --vm=true is required in order to make the ingress work in the minikube on mac os.

- Create service account and role:
```shell
kubectl apply -n ledger -f kubernetes/binding.yaml
```

- Create deployment:
```shell
kubectl apply -n ledger -f kubernetes/deployment.yaml
```

- Create service:
```shell
kubectl apply -n ledger -f kubernetes/service.yaml
```
- Create ingress:

```shell
kubectl apply -n ledger -f kubernetes/ingress.yaml
```

- Scale in/out:
```shell
kubectl scale deployments/ledger --replicas=<desired-replicas>
```

### Akka cluster HTTP API

1 - Listing the cluster members:
```shell
KUBE_IP=$(minikube ip)
MANAGEMENT_PORT=$(kubectl get svc ledger-service  -ojsonpath="{.spec.ports[?(@.name==\"management\")].nodePort}")
curl --silent  http://$KUBE_IP:$MANAGEMENT_PORT/cluster/members | jq
```
