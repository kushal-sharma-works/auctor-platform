# Auctor Dev/SIT Helm Chart

## Clean Structure

**Use these files:**
- `Chart.yaml` - Chart definition
- `values.yaml`, `values-dev.yaml`, `values-sit.yaml` - Configuration
- `*-deployment-dev.yaml` - Deployments for dev/sit
- `services.yaml` - Service definitions (ClusterIP)
- `ingress.yaml` - Ingress routing
- `configmap.yaml` - Environment config
- `deploy-dev.sh` - Simple deployment script
- `destroy.sh` - Cleanup

**Can ignore (prod-only):**
- `*-deployment.yaml` (old files)
- `definition-deployment-new.yaml`, `execution-deployment-new.yaml`
- `deploy.sh`, `INGRESS_SETUP.md`
- `hpa.yaml`, `ingress-controller.yaml`
- `values-prod.yaml`, `values-clean.yaml`

## Quick Deploy

```bash
cd infra/helm
chmod +x deploy-dev.sh
./deploy-dev.sh values-dev.yaml auctor-dev.local
```

Read `README-DEV.md` for full guide.
