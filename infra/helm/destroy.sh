#!/bin/bash
set -e

NAMESPACE="auctor"
RELEASE_NAME="auctor"

echo "Uninstalling Auctor release..."
helm uninstall $RELEASE_NAME -n $NAMESPACE --wait || true

echo "Deleting namespace..."
kubectl delete namespace $NAMESPACE --ignore-not-found=true || true

echo "✓ Auctor deployment removed"
