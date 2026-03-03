# Kubernetes Quickstart Guide

This guide explains how to run Amerbank on Kubernetes using Minikube for local development.

## Prerequisites

- [Minikube](https://minikube.sigs.k8s.io/docs/start/) installed
- [kubectl](https://kubernetes.io/docs/tasks/tools/) installed
- [Docker](https://www.docker.com/) installed
- [Helm](https://helm.sh/) installed

### Install NGINX Ingress Controller

The NGINX Ingress Controller is required for the Ingress resource to work.

```bash
# Add the ingress-nginx repository
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx

# Update helm repositories
helm repo update

# Install the NGINX Ingress Controller
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace
```

Verify the controller is running:

```bash
kubectl get pods -n ingress-nginx
```

## Quickstart

### 1. Start Minikube

It is recommended to allocate at least 4 CPUs and 8GB of RAM to Minikube to run all services smoothly:

```bash
minikube start --cpus=4 --memory=8g
```

### 2. Create the namespace

```bash
kubectl create namespace amerbank
```

### 3. Build and load Docker images

Set your Docker environment to use Minikube's Docker daemon:

```bash
eval $(minikube docker-env)
```

> **Note:** This is a session-scoped change. If you open a new terminal, you will need to run this command again before building or working with images.

Build each service image using `docker build`:

```bash
# Config Server
docker build -t config-server:latest ./services/config-server

# Discovery Server
docker build -t discovery:latest ./services/discovery

# Gateway
docker build -t gateway:latest ./services/gateway

# Auth Server
docker build -t auth-server:latest ./services/auth-server

# Customer Service
docker build -t customer:latest ./services/customer

# Account Service
docker build -t account:latest ./services/account

# Transaction Service
docker build -t transaction:latest ./services/transaction
```

### 4. Deploy infrastructure components

Apply manifests in the following order:

```bash
# PostgreSQL
kubectl apply -f k8s/postgres/

# Redis
kubectl apply -f k8s/redis/
```

Wait for pods to be ready before proceeding:

```bash
kubectl get pods -n amerbank -w
```

### 5. Deploy all services

```bash
kubectl apply -f k8s/ -R
```

> **Note:** This will also re-apply the infrastructure manifests from the previous step, which is safe and expected.

Wait for all pods to be ready:

```bash
kubectl get pods -n amerbank -w
```

### 6. Expose services with Minikube tunnel

Open a new terminal and run:

```bash
minikube tunnel
```

This will expose the LoadBalancer services on localhost. Keep this terminal open for as long as you need the application to be accessible.

### 7. Configure hosts file

Add the following line to your hosts file:

**Windows**: `C:\Windows\System32\drivers\etc\hosts`  
**Linux/Mac**: `/etc/hosts`

```
127.0.0.1  api.local
```

### 8. Access the application

The API Gateway will be available at:

```
http://api.local
```

## Deployment Order Summary

| Step | Component | Command |
|------|-----------|---------|
| 1 | Namespace | `kubectl create namespace amerbank` |
| 2 | Build images | `docker build -t <image>:latest ./services/<service>` |
| 3 | Deploy infrastructure | `kubectl apply -f k8s/postgres/ && kubectl apply -f k8s/redis/` |
| 4 | Deploy all services | `kubectl apply -f k8s/ -R` |

## Stopping the Cluster

To stop Minikube without deleting any resources:

```bash
minikube stop
```

To restart it later, simply run `minikube start` and resume from step 6.

To fully tear down all Amerbank resources:

```bash
kubectl delete namespace amerbank
```

To delete the Minikube cluster entirely:

```bash
minikube delete
```

## Troubleshooting

### Check pod status

```bash
kubectl get pods -n amerbank
```

### View pod logs

```bash
kubectl logs <pod-name> -n amerbank
```

### Describe a pod for details

```bash
kubectl describe pod <pod-name> -n amerbank
```

### Check services

```bash
kubectl get svc -n amerbank
```

### Check ingress

```bash
kubectl get ingress -n amerbank
```

### Restart a deployment

```bash
kubectl rollout restart deployment <deployment-name> -n amerbank
```

### Minikube tunnel issues

If the tunnel stops working:

1. Stop the current tunnel (Ctrl+C)
2. Restart it:

```bash
minikube tunnel
```

### Common issues

- **Pod stuck in Pending**: Check storage class or PVC availability
- **Pod stuck in ImagePullBackOff**: Verify images are built and loaded into Minikube's Docker daemon (`eval $(minikube docker-env)`)
- **Service unreachable**: Ensure `minikube tunnel` is running in a separate terminal
- **Ingress not working**: Verify the ingress controller is installed (`kubectl get pods -n ingress-nginx`)