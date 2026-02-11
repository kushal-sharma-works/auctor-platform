terraform {
  # Configure backend for remote state storage in Azure
  # Initialize with: terraform init -backend-config="resource_group_name=<rg>"
  backend "azurerm" {
    resource_group_name  = "rg-terraform-state"
    storage_account_name = "auctorstatestore"
    container_name       = "tfstate"
    key                  = "auctor/terraform.tfstate"
  }
}
