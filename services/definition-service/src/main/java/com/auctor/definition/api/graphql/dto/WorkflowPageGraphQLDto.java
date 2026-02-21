package com.auctor.definition.api.graphql.dto;

import org.springframework.data.domain.Page;
import com.auctor.definition.domain.model.WorkflowDefinition;

import java.util.List;

/**
 * GraphQL DTO for paginated workflow results.
 */
public record WorkflowPageGraphQLDto(
    List<WorkflowGraphQLDto> content,
    Integer totalElements,
    Integer totalPages,
    Integer page,
    Integer size
) {
    public static WorkflowPageGraphQLDto from(Page<WorkflowDefinition> page) {
        return new WorkflowPageGraphQLDto(
            page.getContent().stream()
                .map(WorkflowGraphQLDto::from)
                .toList(),
            (int) page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }
}
