package com.auctor.definition.api.rest;

import com.auctor.definition.api.rest.dto.CreateWorkflowRequest;
import com.auctor.definition.api.rest.dto.WorkflowResponse;
import com.auctor.definition.api.rest.mapper.DtoMapper;
import com.auctor.definition.domain.model.Transition;
import com.auctor.definition.domain.model.WorkflowDefinition;
import com.auctor.definition.domain.model.WorkflowId;
import com.auctor.definition.domain.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for workflow operations.
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {
    
    private final WorkflowService workflowService;
    
    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }
    
    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        List<Transition> transitions = request.transitions().stream()
            .map(DtoMapper::toDomain)
            .collect(Collectors.toList());
        
        WorkflowDefinition workflow = workflowService.create(
            request.name(),
            request.states(),
            request.initialState(),
            transitions
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(DtoMapper.toResponse(workflow));
    }
    
    @GetMapping
    public ResponseEntity<Page<WorkflowResponse>> listWorkflows(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<WorkflowDefinition> workflows = workflowService.listAll(PageRequest.of(page, size));
        return ResponseEntity.ok(workflows.map(DtoMapper::toResponse));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflow(@PathVariable String id) {
        WorkflowDefinition workflow = workflowService.getById(new WorkflowId(id));
        return ResponseEntity.ok(DtoMapper.toResponse(workflow));
    }
    
    @GetMapping("/{id}/versions/{version}")
    public ResponseEntity<WorkflowResponse> getWorkflowVersion(
        @PathVariable String id,
        @PathVariable int version
    ) {
        WorkflowDefinition workflow = workflowService.getByIdAndVersion(new WorkflowId(id), version);
        return ResponseEntity.ok(DtoMapper.toResponse(workflow));
    }
    
    @PostMapping("/{id}/publish")
    public ResponseEntity<WorkflowResponse> publishWorkflow(@PathVariable String id) {
        WorkflowDefinition workflow = workflowService.publish(new WorkflowId(id));
        return ResponseEntity.ok(DtoMapper.toResponse(workflow));
    }
}
