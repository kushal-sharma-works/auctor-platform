terraform {
  required_version = ">= 1.6.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.116"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "azurerm" {
  features {}
}

data "azurerm_client_config" "current" {}

resource "random_string" "suffix" {
  length  = 4
  upper   = false
  special = false
}

locals {
  env_prefix        = "${var.name_prefix}-${var.environment}"
  rg_name           = "${local.env_prefix}-rg"
  aks_name          = "${local.env_prefix}-aks"
  pg_name           = "${local.env_prefix}-pg"
  redis_name        = "${local.env_prefix}-redis"
  workload_location = var.workload_location != "" ? var.workload_location : var.location

  # Simple name generation: ACR names must be lowercase alphanumeric only
  acr_name = lower(replace(replace("${var.name_prefix}${var.environment}${random_string.suffix.result}", "-", ""), "_", ""))
  # Key Vault names must be lowercase alphanumeric and hyphens, max 24 chars
  kv_name = substr(lower(replace("${var.name_prefix}-${var.environment}-${random_string.suffix.result}", "_", "")), 0, 24)
}

resource "azurerm_resource_group" "rg" {
  name     = local.rg_name
  location = var.location
}

resource "azurerm_kubernetes_cluster" "aks" {
  name                = local.aks_name
  location            = local.workload_location
  resource_group_name = azurerm_resource_group.rg.name
  dns_prefix          = var.aks_dns_prefix
  kubernetes_version  = var.kubernetes_version != "" ? var.kubernetes_version : null

  default_node_pool {
    name       = "system"
    node_count = var.node_count
    vm_size    = var.vm_size
  }

  identity {
    type = "SystemAssigned"
  }
}

resource "azurerm_container_registry" "acr" {
  name                = local.acr_name
  resource_group_name = azurerm_resource_group.rg.name
  location            = azurerm_resource_group.rg.location
  sku                 = var.acr_sku
  admin_enabled       = false
}

# Grant AKS permission to pull images from ACR
resource "azurerm_role_assignment" "aks_acr_pull" {
  principal_id                     = azurerm_kubernetes_cluster.aks.kubelet_identity[0].object_id
  role_definition_name             = "AcrPull"
  scope                            = azurerm_container_registry.acr.id
  skip_service_principal_aad_check = true
}

resource "azurerm_postgresql_flexible_server" "postgres" {
  name                   = local.pg_name
  resource_group_name    = azurerm_resource_group.rg.name
  location               = local.workload_location
  version                = var.postgres_version
  administrator_login    = var.postgres_admin_username
  administrator_password = var.postgres_admin_password
  storage_mb             = var.postgres_storage_mb
  sku_name               = var.postgres_sku
  backup_retention_days  = var.postgres_backup_retention_days
  zone                   = var.postgres_zone

  public_network_access_enabled = var.postgres_public_network_access_enabled

  dynamic "high_availability" {
    for_each = var.postgres_enable_high_availability ? [1] : []
    content {
      mode                      = "ZoneRedundant"
      standby_availability_zone = var.postgres_standby_zone
    }
  }
}

resource "azurerm_postgresql_flexible_server_database" "definition" {
  name      = "definition"
  server_id = azurerm_postgresql_flexible_server.postgres.id
  collation = "en_US.utf8"
  charset   = "UTF8"
}

resource "azurerm_postgresql_flexible_server_database" "execution" {
  name      = "execution"
  server_id = azurerm_postgresql_flexible_server.postgres.id
  collation = "en_US.utf8"
  charset   = "UTF8"
}

resource "azurerm_redis_cache" "redis" {
  name                = local.redis_name
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  capacity            = var.redis_capacity
  family              = var.redis_family
  sku_name            = var.redis_sku
  minimum_tls_version = "1.2"
}

resource "azurerm_key_vault" "kv" {
  name                       = local.kv_name
  location                   = azurerm_resource_group.rg.location
  resource_group_name        = azurerm_resource_group.rg.name
  tenant_id                  = data.azurerm_client_config.current.tenant_id
  sku_name                   = "standard"
  purge_protection_enabled   = true
  soft_delete_retention_days = 7
  enable_rbac_authorization  = true
}
