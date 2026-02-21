terraform {
  # Configure backend for remote state storage in Azure.
  # Recommended: pass real values via -backend-config, for example:
  #   terraform init \
  #     -backend-config="resource_group_name=<RESOURCE_GROUP_NAME>" \
  #     -backend-config="storage_account_name=<STORAGE_ACCOUNT_NAME>" \
  #     -backend-config="container_name=<CONTAINER_NAME>" \
  #     -backend-config="key=<STATE_KEY>"
  backend "azurerm" {
    resource_group_name  = "<RESOURCE_GROUP_NAME>"
    storage_account_name = "<STORAGE_ACCOUNT_NAME>"
    container_name       = "<CONTAINER_NAME>"
    key                  = "<STATE_KEY>"
  }
}
