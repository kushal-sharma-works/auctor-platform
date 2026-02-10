# Argo CD

Manifests to deploy the Auctor Helm chart using Argo CD.

## Install Argo CD
```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

## Apply Project and Applications
```bash
kubectl apply -n argocd -f infra/argocd/project.yaml
kubectl apply -n argocd -f infra/argocd/application-sit.yaml
```

## Optional: Dev Environment
```bash
kubectl apply -n argocd -f infra/argocd/application-dev.yaml
```

## Notes
- `application-sit.yaml` is the primary deployment target.
- Update `repoURL` if you fork the repository.
