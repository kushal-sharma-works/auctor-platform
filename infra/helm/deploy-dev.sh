#!/bin/bash
set -e

NAMESPACE="auctor"
RELEASE_NAME="auctor"
HELM_CHART_PATH="./infra/helm"
VALUES_FILE="${1:-values-dev.yaml}"
DOMAIN="${2:-auctor.local}"
VALIDATE="${3:---validate=true}"

echo "==========================================="
echo "Auctor Dev/SIT Deployment"
echo "==========================================="
echo "Namespace: $NAMESPACE"
echo "Values: $VALUES_FILE"
echo "Domain: $DOMAIN"
echo "==========================================="

# Create namespace
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f - || true

# Lint chart
helm lint "$HELM_CHART_PATH"

# Dry run
echo ""
echo "Running dry-run..."
helm upgrade --install $RELEASE_NAME "$HELM_CHART_PATH" \
    --namespace $NAMESPACE \
    --values "$HELM_CHART_PATH/$VALUES_FILE" \
    --set ingress.host="$DOMAIN" \
    --dry-run --debug $VALIDATE

# Deploy
echo ""
echo "Deploying..."
helm upgrade --install $RELEASE_NAME "$HELM_CHART_PATH" \
    --namespace $NAMESPACE \
    --values "$HELM_CHART_PATH/$VALUES_FILE" \
    --set ingress.host="$DOMAIN" \
    --wait $VALIDATE

echo ""
echo "✓ Deployment complete!"
echo ""
echo "Check status:"
echo "  kubectl get all -n $NAMESPACE"
echo ""
echo "Ingress IP:"
echo "  kubectl get ingress -n $NAMESPACE"
echo ""
echo "Logs:"
echo "  kubectl logs -n $NAMESPACE -l app.kubernetes.io/name=definition-service -f"
echo "  kubectl logs -n $NAMESPACE -l app.kubernetes.io/name=execution-service -f"
echo "  kubectl logs -n $NAMESPACE -l app.kubernetes.io/name=web -f"
