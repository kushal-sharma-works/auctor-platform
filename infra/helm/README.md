# Auctor Helm Chart

Helm chart for the Auctor platform with environment overlays for dev and SIT.

## Prerequisites
- Kubernetes 1.26+
- Helm 3
- Ingress controller (nginx recommended)

## Quick Start (SIT)
```bash
cd infra/helm
helm upgrade --install auctor-sit . -n auctor --create-namespace -f values-sit.yaml \
	--set ingress.host=auctor-platform.sit.example.com
```

## Dev Install
```bash
cd infra/helm
helm upgrade --install auctor-dev . -n auctor --create-namespace -f values-dev.yaml
```

## Values Overview
- `image.registry`, `image.tag` control image source.
- `ingress.host` sets the domain.
- `config.*` provides non-sensitive runtime configuration.
- `secrets.*` stores database passwords and JWT secret (use Key Vault in prod).
- `hpa.execution.*` controls autoscaling for execution-service.

## Chart Structure
```
infra/helm/
├── Chart.yaml
├── values.yaml
├── values-dev.yaml
├── values-sit.yaml
└── templates/
	├── _helpers.tpl
	├── configmap.yaml
	├── secrets.yaml
	├── definition-deployment.yaml
	├── execution-deployment.yaml
	├── web-deployment.yaml
	├── services.yaml
	├── ingress.yaml
	├── hpa.yaml
	└── networkpolicy.yaml
```

## Notes
- The chart uses stable service names derived from the Helm release name.
- For production, replace `secrets.*` with External Secrets or Key Vault CSI.
