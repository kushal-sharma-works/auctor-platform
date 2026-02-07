package com.auctor.definition.domain.port;

import com.auctor.definition.domain.model.PolicyDefinition;
import com.auctor.definition.domain.model.PolicyId;

/**
 * Port for policy command operations (writes).
 */
public interface PolicyCommandPort {
    
    /**
     * Save a policy definition.
     */
    PolicyDefinition save(PolicyDefinition policy);
    
    /**
     * Publish a policy definition.
     */
    PolicyDefinition publish(PolicyId id);
}
