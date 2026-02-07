# Auctor Platform - Dev/SIT Deployment

Simple, clean Helm chart for dev/SIT environments with best practices.

## What's Included

✅ **Ingress** - Single entry point (replace LoadBalancers)
✅ **Health Checks** - Readiness & liveness probes
✅ **Environment Config** - Dev/SIT value overrides
✅ **Proper Logging** - Environment-based log levels
✅ **Resource Limits** - Prevents resource hogging

## Quick Start

### 1. Prerequisites
```bash
# Install Nginx Ingress Controller
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm install ingress-nginx ingress-nginx/ingress-nginx -n ingress-nginx --create-namespace
```

### 2. Deploy to Dev
```bash
cd infra/helm
./deploy-dev.sh values-dev.yaml auctor-dev.local
```

### 3. Deploy to SIT
```bash
./deploy-dev.sh values-sit.yaml auctor-sit.local
```

### 4. Get Ingress IP & Update /etc/hosts
```bash
kubectl get ingress -n auctor

# Update /etc/hosts (Mac/Linux):
# 10.x.x.x auctor-dev.local auctor-sit.local
```

### 5. Access Services
- **Web**: http://auctor-dev.local
- **Definition API**: http://auctor-dev.local/api
- **Execution GraphQL**: http://auctor-dev.local/graphql

## Deploy Commands

```bash
# Dev
./deploy-dev.sh values-dev.yaml auctor-dev.local

# SIT
./deploy-dev.sh values-sit.yaml auctor-sit.local

# Uninstall
./destroy.sh
```

## View Logs

```bash
kubectl logs -n auctor -l app=definition-service -f
kubectl logs -n auctor -l app=execution-service -f
kubectl logs -n auctor -l app=web -f
```

## Port Forward (for local testing without Ingress)

```bash
kubectl port-forward -n auctor svc/definition-service 8081:8081
kubectl port-forward -n auctor svc/execution-service 8082:8082
kubectl port-forward -n auctor svc/web 3000:3000
```

## File Structure

```
infra/helm/
├── Chart.yaml                  # Chart metadata
├── values.yaml                 # Base values
├── values-dev.yaml             # Dev overrides (1 replica)
├── values-sit.yaml             # SIT overrides (2 replicas)
├── definition-deployment-dev.yaml
├── execution-deployment-dev.yaml
├── web-deployment-dev.yaml
├── services.yaml               # ClusterIP services
├── ingress.yaml                # Single Ingress
├── configmap.yaml              # ConfigMap for env config
├── deploy-dev.sh               # Simple deploy script
└── destroy.sh                  # Cleanup script
```

## Environment Variables

Services get `ENVIRONMENT` and `LOG_LEVEL` from values:

**Dev**: `LOG_LEVEL=DEBUG`
**SIT**: `LOG_LEVEL=INFO`

## Customization

Edit `values-dev.yaml` or `values-sit.yaml`:

```yaml
replicaCount: 1
environment: dev
logLevel: DEBUG
ingress:
  host: "your-domain.local"
resources:
  limits:
    cpu: "500m"
    memory: "512Mi"
```
