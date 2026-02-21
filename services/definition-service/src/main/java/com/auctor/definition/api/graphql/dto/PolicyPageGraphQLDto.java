package com.auctor.definition.api.graphql.dto;

import org.springframework.data.domain.Page;
import com.auctor.definition.domain.model.PolicyDefinition;

import java.util.List;

/**
 * GraphQL DTO for paginated policy results.
 */
public record PolicyPageGraphQLDto(
    List<PolicyGraphQLDto> content,
    Integer totalElements,
    Integer totalPages,
    Integer page,
    Integer size
) {
    public static PolicyPageGraphQLDto from(Page<PolicyDefinition> page) {
        return new PolicyPageGraphQLDto(
            page.getContent().stream()
                .map(PolicyGraphQLDto::from)
                .toList(),
            (int) page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }
}
