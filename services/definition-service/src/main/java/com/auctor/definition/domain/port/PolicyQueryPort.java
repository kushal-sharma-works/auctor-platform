package com.auctor.definition.domain.port;

import com.auctor.definition.domain.model.PolicyDefinition;
import com.auctor.definition.domain.model.PolicyId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Port for policy query operations (reads).
 */
public interface PolicyQueryPort {
    
    /**
     * Find a policy by its ID (returns latest version).
     */
    Optional<PolicyDefinition> findById(PolicyId id);
    
    /**
     * Find a policy by ID and specific version.
     */
    Optional<PolicyDefinition> findByIdAndVersion(PolicyId id, int version);
    
    /**
     * Find all policies with pagination.
     */
    Page<PolicyDefinition> findAll(Pageable pageable);
}
