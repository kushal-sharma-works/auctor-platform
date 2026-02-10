package com.auctor.definition.api.graphql;

import com.auctor.definition.api.graphql.dto.PolicyGraphQLDto;
import com.auctor.definition.api.graphql.dto.PolicyPageGraphQLDto;
import com.auctor.definition.api.graphql.input.CreatePolicyInput;
import com.auctor.definition.domain.model.PolicyDefinition;
import com.auctor.definition.domain.model.PolicyId;
import com.auctor.definition.domain.service.PolicyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * GraphQL Controller for Policy operations.
 * Handles queries and mutations for policies.
 */
@Controller
public class PolicyGraphQLController {
    
    private final PolicyService policyService;
    
    public PolicyGraphQLController(PolicyService policyService) {
        this.policyService = policyService;
    }
    
    @QueryMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'ADMIN', 'EXECUTOR')")
    public PolicyGraphQLDto policy(@Argument String id) {
        PolicyDefinition policy = policyService.getById(new PolicyId(id));
        return PolicyGraphQLDto.from(policy);
    }
    
    @QueryMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'ADMIN', 'EXECUTOR')")
    public PolicyPageGraphQLDto policies(
        @Argument Integer page,
        @Argument Integer size
    ) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        
        Page<PolicyDefinition> policyPage = policyService.listAll(
            PageRequest.of(pageNum, pageSize)
        );
        
        return PolicyPageGraphQLDto.from(policyPage);
    }
    
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PolicyGraphQLDto createPolicy(@Argument CreatePolicyInput input) {
        PolicyDefinition policy = policyService.create(
            input.name(),
            input.conditions().stream()
                .map(c -> c.toDomain())
                .toList()
        );
        
        return PolicyGraphQLDto.from(policy);
    }
    
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PolicyGraphQLDto publishPolicy(@Argument String id) {
        PolicyDefinition policy = policyService.publish(new PolicyId(id));
        return PolicyGraphQLDto.from(policy);
    }
}
