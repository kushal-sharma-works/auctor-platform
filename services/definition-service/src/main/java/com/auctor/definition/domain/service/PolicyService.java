package com.auctor.definition.domain.service;

import com.auctor.definition.domain.exception.EntityNotFoundException;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.domain.port.PolicyCommandPort;
import com.auctor.definition.domain.port.PolicyQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain service for policy operations.
 * Contains business logic for policy lifecycle.
 */
public class PolicyService {
    
    private final PolicyCommandPort commandPort;
    private final PolicyQueryPort queryPort;
    
    public PolicyService(PolicyCommandPort commandPort, PolicyQueryPort queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }
    
    /**
     * Create a new policy definition.
     */
    public PolicyDefinition create(String name, List<PolicyCondition> conditions) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        
        PolicyDefinition policy = new PolicyDefinition(
            new PolicyId(id),
            name,
            1,
            new PolicyStatus.Draft(),
            conditions,
            now
        );
        
        return commandPort.save(policy);
    }
    
    /**
     * Publish a policy definition.
     * Validates that the policy is in DRAFT status.
     */
    public PolicyDefinition publish(PolicyId id) {
        PolicyDefinition policy = queryPort.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Policy", id.value()));
        
        // Validate policy is in DRAFT status
        if (!(policy.status() instanceof PolicyStatus.Draft)) {
            throw new IllegalArgumentException("Policy must be in DRAFT status to publish");
        }
        
        // Update status to PUBLISHED and save
        PolicyDefinition publishedPolicy = policy.withStatus(new PolicyStatus.Published());
        return commandPort.save(publishedPolicy);
    }
    
    /**
     * Get policy by ID (latest version).
     */
    public PolicyDefinition getById(PolicyId id) {
        return queryPort.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Policy", id.value()));
    }
    
    /**
     * Get policy by ID and specific version.
     */
    public PolicyDefinition getByIdAndVersion(PolicyId id, int version) {
        return queryPort.findByIdAndVersion(id, version)
            .orElseThrow(() -> new EntityNotFoundException(
                "Policy not found with id: " + id.value() + " and version: " + version
            ));
    }
    
    /**
     * List all policies with pagination.
     */
    public Page<PolicyDefinition> listAll(Pageable pageable) {
        return queryPort.findAll(pageable);
    }
}
