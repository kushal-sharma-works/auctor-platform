package com.auctor.definition.api.graphql;

import com.auctor.definition.api.graphql.dto.WorkflowGraphQLDto;
import com.auctor.definition.api.graphql.dto.WorkflowPageGraphQLDto;
import com.auctor.definition.api.graphql.input.CreateWorkflowInput;
import com.auctor.definition.domain.model.WorkflowDefinition;
import com.auctor.definition.domain.model.WorkflowId;
import com.auctor.definition.domain.service.WorkflowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * GraphQL Controller for Workflow operations.
 * Handles queries and mutations for workflows.
 */
@Controller
public class WorkflowGraphQLController {
    
    private final WorkflowService workflowService;
    
    public WorkflowGraphQLController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }
    
    @QueryMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'ADMIN', 'EXECUTOR')")
    public WorkflowGraphQLDto workflow(@Argument String id) {
        WorkflowDefinition workflow = workflowService.getById(new WorkflowId(id));
        return WorkflowGraphQLDto.from(workflow);
    }
    
    @QueryMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'ADMIN', 'EXECUTOR')")
    public WorkflowPageGraphQLDto workflows(
        @Argument Integer page,
        @Argument Integer size
    ) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        
        Page<WorkflowDefinition> workflowPage = workflowService.listAll(
            PageRequest.of(pageNum, pageSize)
        );
        
        return WorkflowPageGraphQLDto.from(workflowPage);
    }
    
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public WorkflowGraphQLDto createWorkflow(@Argument CreateWorkflowInput input) {
        WorkflowDefinition workflow = workflowService.create(
            input.name(),
            input.states(),
            input.initialState(),
            input.transitions().stream()
                .map(t -> t.toDomain())
                .toList()
        );
        
        return WorkflowGraphQLDto.from(workflow);
    }
    
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public WorkflowGraphQLDto publishWorkflow(@Argument String id) {
        WorkflowDefinition workflow = workflowService.publish(new WorkflowId(id));
        return WorkflowGraphQLDto.from(workflow);
    }
}
