# Azure Terraform

Terraform stack for the Auctor platform on Azure, designed for a single SIT environment.

## What Gets Created
- Resource Group
- AKS cluster
- Azure Container Registry (ACR)
- PostgreSQL Flexible Server with `definition` and `execution` databases
- Azure Cache for Redis
- Azure Key Vault

## Prerequisites
- Terraform 1.6+
- Azure CLI logged in (`az login`)
- Subscription permissions to create AKS, ACR, Postgres, Redis, Key Vault

## Quick Start
```bash
cd infra/terraform/azure
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with real values
terraform init -backend=false
terraform validate
terraform plan
terraform apply
```

## Remote State (Recommended)
Uncomment and configure the backend block in `backend.tf` for Azure Storage state.

## Outputs
- `kubeconfig_raw` (sensitive)
- `acr_login_server`
- `postgres_definition_connection_string` (sensitive)
- `postgres_execution_connection_string` (sensitive)
