# Production Deployment Guide

## Prerequisites

### Required Tools
- Azure CLI (`az`)
- kubectl
- Helm 3.16+
- Terraform 1.6+
- Docker

### Azure Resources
- Azure subscription with appropriate permissions
- Resource group for Terraform state storage

## Initial Setup

### 1. Configure Terraform Backend

Create storage account for Terraform state:
```bash
# Create resource group
az group create --name rg-terraform-state --location eastus

# Create storage account
az storage account create \\
  --name auctorstatestore \\
  --resource-group rg-terraform-state \\
  --sku Standard_LRS \\
  --encryption-services blob

# Create container
az storage container create \\
  --name tfstate \\
  --account-name auctorstatestore
```

### 2. Provision Infrastructure

```bash
cd infra/terraform/azure

# Initialize Terraform with backend
terraform init

# Review plan
terraform plan -out=tfplan

# Apply infrastructure
terraform apply tfplan
```

**Resources Created:**
- AKS cluster with system-assigned identity
- Azure Container Registry with AcrPull role
- PostgreSQL Flexible Server (HA with zone redundancy)
- Redis Cache
- Azure Key Vault
- Virtual Network and subnets

### 3. Configure kubectl

```bash
# Get AKS credentials
az aks get-credentials \\
  --resource-group <rg-name> \\
  --name <aks-name>

# Verify connectivity
kubectl get nodes
```

### 4. Build and Push Images

```bash
# Login to ACR
az acr login --name <acr-name>

# Build services
docker build -t <acr-login-server>/definition-service:v1.0.0 services/definition-service
docker build -t <acr-login-server>/execution-service:v1.0.0 services/execution-service
docker build -t <acr-login-server>/web:v1.0.0 web

# Push images
docker push <acr-login-server>/definition-service:v1.0.0
docker push <acr-login-server>/execution-service:v1.0.0
docker push <acr-login-server>/web:v1.0.0
```

## Deploy Application

### Using Helm (Recommended)

```bash
cd infra/helm

# Create namespace
kubectl create namespace auctor

# Deploy to SIT
helm upgrade --install auctor-sit . \\
  --namespace auctor \\
  --create-namespace \\
  -f values-sit.yaml \\
  --set image.registry=<acr-login-server> \\
  --set image.tag=v1.0.0 \\
  --set ingress.host=auctor-platform.sit.example.com \\
  --set secrets.definitionDbPassword=<password> \\
  --set secrets.executionDbPassword=<password> \\
  --set secrets.definitionJwtSecret=<secret>
```

### Using GitHub Actions

Trigger the Deploy - SIT workflow:
1. Go to Actions tab
2. Select "Deploy - SIT"
3. Click "Run workflow"
4. Enter parameters:
   - git_ref: `main`
   - image_tag: `v1.0.0` (or leave empty for git SHA)
   - ingress_host: Your domain

## Post-Deployment

### 1. Verify Deployments

```bash
# Check pods
kubectl get pods -n auctor

# Check services
kubectl get svc -n auctor

# Check ingress
kubectl get ingress -n auctor

# View logs
kubectl logs -n auctor -l app.kubernetes.io/name=definition-service --tail=100
```

### 2. Configure DNS

Get ingress IP:
```bash
kubectl get ingress -n auctor -o jsonpath='{.items[0].status.loadBalancer.ingress[0].ip}'
```

Create DNS A record pointing your domain to this IP.

### 3. Health Checks

```bash
# Definition service
curl https://auctor-platform.sit.example.com/api/actuator/health

# Execution service
curl https://auctor-platform.sit.example.com/graphql

# Web UI
curl https://auctor-platform.sit.example.com/
```

## Security Enhancements

### Azure Key Vault Integration (Recommended)

1. Install CSI driver:
```bash
helm repo add csi-secrets-store-provider-azure \\
  https://azure.github.io/secrets-store-csi-driver-provider-azure/charts

helm install csi-secrets-store csi-secrets-store-provider-azure/csi-secrets-store-provider-azure \\
  --namespace kube-system
```

2. Create SecretProviderClass (see `infra/helm/templates/secretprovider.yaml.example`)

3. Update deployment to mount secrets from Key Vault

### Enable TLS/SSL

1. Install cert-manager:
```bash
helm install cert-manager jetstack/cert-manager \\
  --namespace cert-manager \\
  --create-namespace \\
  --set installCRDs=true
```

2. Create ClusterIssuer for Let's Encrypt
3. Update ingress annotations:
```yaml
cert-manager.io/cluster-issuer: letsencrypt-prod
```

## Monitoring Setup

### Access Dashboards

- **Prometheus**: Port-forward or expose via ingress
  ```bash
  kubectl port-forward -n auctor svc/prometheus 9090:9090
  ```

- **Grafana**: Import dashboard from `infra/monitoring/grafana/dashboards/`
  ```bash
  kubectl port-forward -n auctor svc/grafana 3000:3000
  # Login with admin/admin
  ```

### Configure Alerting

1. Deploy Alertmanager (optional)
2. Configure notification channels (Slack, email, PagerDuty)
3. Test alerts:
   ```bash
   kubectl delete pod -n auctor -l app.kubernetes.io/name=definition-service
   ```

## Scaling

### Manual Scaling

```bash
# Scale deployment
kubectl scale deployment auctor-sit-definition-service -n auctor --replicas=3

# Update HPA
kubectl edit hpa auctor-sit-definition-service -n auctor
```

### Auto-scaling Configuration

HPA is configured by default. Adjust in `values-sit.yaml`:
```yaml
hpa:
  definition:
    minReplicas: 2
    maxReplicas: 10
    targetCPUUtilizationPercentage: 70
```

## Maintenance

### Rolling Updates

```bash
helm upgrade auctor-sit . \\
  -f values-sit.yaml \\
  --set image.tag=v1.1.0
```

### Database Migrations

Flyway migrations run automatically on definition-service startup.

### Backup Verification

Monthly test (see [disaster-recovery.md](disaster-recovery.md)):
```bash
az postgres flexible-server restore \\
  --resource-group test-rg \\
  --name test-restore-$(date +%Y%m%d) \\
  --source-server <prod-server-id>
```

## Troubleshooting

### Pods not starting

```bash
kubectl describe pod <pod-name> -n auctor
kubectl logs <pod-name> -n auctor
```

Common issues:
- Image pull errors → Check ACR permissions
- CrashLoopBackOff → Check logs for application errors
- Database connection → Verify network policies

### High latency

1. Check HPA status: `kubectl get hpa -n auctor`
2. Review metrics: Access Grafana dashboard
3. Check database connection pool: Review app metrics

### Certificate issues

```bash
kubectl describe certificate -n auctor
kubectl describe certificaterequest -n auctor
```

## Rollback

```bash
# Rollback to previous release
helm rollback auctor-sit -n auctor

# Rollback to specific revision
helm rollback auctor-sit 2 -n auctor
```

## Clean Up

**Warning**: This will delete all resources!

```bash
# Delete Helm release
helm uninstall auctor-sit -n auctor

# Delete namespace
kubectl delete namespace auctor

# Destroy infrastructure
cd infra/terraform/azure
terraform destroy
```

## Support

- **Documentation**: See `docs/` directory
- **Issues**: GitHub Issues
- **Security**: See SECURITY.md for reporting vulnerabilities
