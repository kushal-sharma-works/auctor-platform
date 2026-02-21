output "kubeconfig_raw" {
  description = "Raw kubeconfig for the AKS cluster."
  value       = azurerm_kubernetes_cluster.aks.kube_config_raw
  sensitive   = true
}

output "acr_login_server" {
  description = "Azure Container Registry login server."
  value       = azurerm_container_registry.acr.login_server
}

output "postgres_definition_connection_string" {
  description = "Connection string for the definition database."
  value       = "postgresql://${azurerm_postgresql_flexible_server.postgres.administrator_login}:${var.postgres_admin_password}@${azurerm_postgresql_flexible_server.postgres.fqdn}:5432/definition?sslmode=require"
  sensitive   = true
}

output "postgres_execution_connection_string" {
  description = "Connection string for the execution database."
  value       = "postgresql://${azurerm_postgresql_flexible_server.postgres.administrator_login}:${var.postgres_admin_password}@${azurerm_postgresql_flexible_server.postgres.fqdn}:5432/execution?sslmode=require"
  sensitive   = true
}
