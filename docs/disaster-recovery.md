# Disaster Recovery Runbook

## Overview
This runbook provides procedures for backup, restore, and disaster recovery scenarios for the Auctor platform.

## Regular Backups

### PostgreSQL Backups
Azure PostgreSQL Flexible Server provides automated backups:
- **Frequency**: Continuous (transaction logs) + Daily full backups
- **Retention**: 30 days (configurable via Terraform)
- **Location**: Azure geo-redundant storage

### Verify Backup Status
```bash
az postgres flexible-server show \\
  --resource-group <rg-name> \\
  --name <server-name> \\
  --query "backup.{retention:backupRetentionDays,geoRedundant:geoRedundantBackup}"
```

### Manual Backup
```bash
# Export specific database
pg_dump -h <server-fqdn> -U <username> -d definition > definition_backup.sql

# For execution database
pg_dump -h <server-fqdn> -U <username> -d execution > execution_backup.sql
```

## Restore Procedures

### Point-in-Time Restore (PITR)
Restore to any point within the backup retention period:

```bash
az postgres flexible-server restore \\
  --resource-group <rg-name> \\
  --name <new-server-name> \\
  --source-server <source-server-id> \\
  --restore-time "2024-02-11T10:00:00Z"
```

### Restore from Manual Backup
```bash
# Create new database if needed
psql -h <server-fqdn> -U <username> -c "CREATE DATABASE definition_restored;"

# Restore data
psql -h <server-fqdn> -U <username> -d definition_restored < definition_backup.sql
```

## Disaster Scenarios

### Scenario 1: Complete AKS Cluster Failure

**Recovery Steps:**
1. Provision new AKS cluster via Terraform:
```bash
cd infra/terraform/azure
terraform apply -target=azurerm_kubernetes_cluster.aks
```

2. Restore PostgreSQL if needed (see above)

3. Deploy application via Helm:
```bash
cd infra/helm
helm upgrade --install auctor-sit . -f values-sit.yaml \\
  --set image.registry=$ACR_LOGIN_SERVER \\
  --set image.tag=$LAST_KNOWN_GOOD_TAG
```

4. Verify services:
```bash
kubectl get pods -n auctor
kubectl get svc -n auctor
```

**RPO**: Minutes (database has continuous backup)  
**RTO**: 20-30 minutes

### Scenario 2: Database Corruption

**Recovery Steps:**
1. Identify corruption time
2. Perform PITR to point before corruption
3. Update connection strings in Helm values
4. Restart application pods:
```bash
kubectl rollout restart deployment -n auctor
```

**RPO**: Up to corruption detection time  
**RTO**: 15-20 minutes

### Scenario 3: ACR Image Loss

**Recovery Steps:**
1. Rebuild images from source:
```bash
docker build -t $ACR_LOGIN_SERVER/definition-service:$TAG services/definition-service
docker build -t $ACR_LOGIN_SERVER/execution-service:$TAG services/execution-service
docker build -t $ACR_LOGIN_SERVER/web:$TAG web
```

2. Push to ACR:
```bash
docker push $ACR_LOGIN_SERVER/definition-service:$TAG
# Repeat for other services
```

3. Pull from backup registry if available

**RPO**: None (images rebuilt from source)  
**RTO**: 30-45 minutes

### Scenario 4: Regional Azure Outage

**Preparation Required:**
- Configure geo-redundant PostgreSQL backup
- Replicate ACR to secondary region
- Deploy to multi-region AKS

**Recovery Steps:**
1. Restore PostgreSQL in secondary region:
```bash
az postgres flexible-server geo-restore \\
  --resource-group <rg-name> \\
  --name <new-server-name> \\
  --source-server <source-server-id> \\
  --location <secondary-region>
```

2. Deploy AKS in secondary region via Terraform
3. Update DNS to point to new region
4. Deploy application

**RPO**: Minutes to hours (depends on replication lag)  
**RTO**: 1-2 hours

### Scenario 5: Accidental Data Deletion

**Recovery Steps:**
1. Determine deletion time
2. Perform PITR before deletion
3. Export specific tables/data:
```bash
pg_dump -h <restored-server-fqdn> -U <username> -d definition -t <table-name> > table_backup.sql
```

4. Import to production:
```bash
psql -h <prod-server-fqdn> -U <username> -d definition < table_backup.sql
```

**RPO**: Up to a few minutes  
**RTO**: 30-60 minutes

## Testing Backup & Restore

### Monthly Backup Test
1. Perform test restore to separate environment:
```bash
az postgres flexible-server restore \\
  --resource-group test-rg \\
  --name test-restore-$(date +%Y%m%d) \\
  --source-server <prod-server-id>
```

2. Verify data integrity:
```bash
psql -h <test-server-fqdn> -U <username> -d definition -c "SELECT COUNT(*) FROM definitions;"
```

3. Document results in `test-results/backup-test-YYYYMMDD.md`

4. Clean up test resources:
```bash
az postgres flexible-server delete --resource-group test-rg --name test-restore-...
```

## Configuration Backup

### Kubernetes Manifests
```bash
# Backup all Kubernetes resources
kubectl get all,configmaps,secrets,pvc -n auctor -o yaml > k8s-backup-$(date +%Y%m%d).yaml
```

### Helm Values
```bash
helm get values auctor-sit -n auctor > helm-values-backup-$(date +%Y%m%d).yaml
```

### Terraform State
Automatically backed up to Azure Storage (configured in backend.tf)

## Monitoring & Alerts

### Backup Health Monitoring
Set up alerts for:
- Backup failures
- Long-running backup operations
- Storage space for backups

```bash
# Check latest backup
az postgres flexible-server backup list \\
  --resource-group <rg-name> \\
  --name <server-name>
```

## Contact Information

**On-Call Engineer**: [Configure PagerDuty/On-call rotation]  
**Azure Support**: [Support plan details]  
**Escalation Path**: [Define escalation procedure]

## Post-Incident

After any disaster recovery:
1. Document incident timeline
2. Review RTO/RPO achievement
3. Update runbook with lessons learned
4. Test improvements in next DR drill
5. Update stakeholders

## Regular Drills

**Quarterly**: Test database restore  
**Bi-annually**: Full cluster failover drill  
**Annually**: Multi-region failover test
