variable "location" {
  description = "Azure region to deploy resources into."
  type        = string
  default     = "eastus"
}

variable "workload_location" {
  description = "Optional region for workload resources (AKS/PostgreSQL). Leave empty to use location."
  type        = string
  default     = ""
}

variable "environment" {
  description = "Deployment environment name (e.g., sit)."
  type        = string
  default     = "sit"
}

variable "name_prefix" {
  description = "Prefix for resource naming."
  type        = string
  default     = "auctor"
}

variable "aks_dns_prefix" {
  description = "DNS prefix for AKS cluster."
  type        = string
  default     = "auctor"
}

variable "kubernetes_version" {
  description = "AKS Kubernetes version. Leave empty to use Azure default supported version for the region."
  type        = string
  default     = ""
}

variable "node_count" {
  description = "AKS default node pool size."
  type        = number
  default     = 2
}

variable "vm_size" {
  description = "AKS node VM size."
  type        = string
  default     = "Standard_DS2_v2"
}

variable "acr_sku" {
  description = "Azure Container Registry SKU."
  type        = string
  default     = "Basic"
}

variable "postgres_version" {
  description = "PostgreSQL flexible server version."
  type        = string
  default     = "16"
}

variable "postgres_sku" {
  description = "PostgreSQL SKU name."
  type        = string
  default     = "B_Standard_B1ms"
}

variable "postgres_storage_mb" {
  description = "PostgreSQL storage in MB."
  type        = number
  default     = 32768
}

variable "postgres_backup_retention_days" {
  description = "PostgreSQL backup retention in days."
  type        = number
  default     = 30
}

variable "postgres_zone" {
  description = "PostgreSQL availability zone."
  type        = string
  default     = "1"
}

variable "postgres_standby_zone" {
  description = "PostgreSQL standby zone for HA."
  type        = string
  default     = "2"
}

variable "postgres_enable_high_availability" {
  description = "Enable PostgreSQL high availability (requires compatible non-burstable SKU)."
  type        = bool
  default     = false
}

variable "postgres_public_network_access_enabled" {
  description = "Enable public network access for PostgreSQL flexible server."
  type        = bool
  default     = true
}

variable "postgres_admin_username" {
  description = "PostgreSQL admin username."
  type        = string
  default     = "auctor_admin"
}

variable "postgres_admin_password" {
  description = "PostgreSQL admin password."
  type        = string
  sensitive   = true

  validation {
    condition = (
      length(var.postgres_admin_password) >= 16 &&
      can(regex("[A-Z]", var.postgres_admin_password)) &&
      can(regex("[a-z]", var.postgres_admin_password)) &&
      can(regex("[0-9]", var.postgres_admin_password))
    )
    error_message = "postgres_admin_password must be at least 16 characters and include uppercase, lowercase, and numeric characters."
  }
}

variable "redis_capacity" {
  description = "Redis cache capacity."
  type        = number
  default     = 1
}

variable "redis_family" {
  description = "Redis cache family."
  type        = string
  default     = "C"
}

variable "redis_sku" {
  description = "Redis cache SKU (Basic, Standard, Premium)."
  type        = string
  default     = "Basic"
}
