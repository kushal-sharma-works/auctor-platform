package com.auctor.definition.api.rest;

import com.auctor.definition.api.rest.dto.CreatePolicyRequest;
import com.auctor.definition.api.rest.dto.PolicyResponse;
import com.auctor.definition.api.rest.mapper.DtoMapper;
import com.auctor.definition.domain.model.PolicyCondition;
import com.auctor.definition.domain.model.PolicyDefinition;
import com.auctor.definition.domain.model.PolicyId;
import com.auctor.definition.domain.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for policy operations.
 */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {
    
    private final PolicyService policyService;
    
    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }
    
    @PostMapping
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        List<PolicyCondition> conditions = request.conditions().stream()
            .map(DtoMapper::toDomain)
            .collect(Collectors.toList());
        
        PolicyDefinition policy = policyService.create(request.name(), conditions);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(DtoMapper.toResponse(policy));
    }
    
    @GetMapping
    public ResponseEntity<Page<PolicyResponse>> listPolicies(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<PolicyDefinition> policies = policyService.listAll(PageRequest.of(page, size));
        return ResponseEntity.ok(policies.map(DtoMapper::toResponse));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PolicyResponse> getPolicy(@PathVariable String id) {
        PolicyDefinition policy = policyService.getById(new PolicyId(id));
        return ResponseEntity.ok(DtoMapper.toResponse(policy));
    }
    
    @GetMapping("/{id}/versions/{version}")
    public ResponseEntity<PolicyResponse> getPolicyVersion(
        @PathVariable String id,
        @PathVariable int version
    ) {
        PolicyDefinition policy = policyService.getByIdAndVersion(new PolicyId(id), version);
        return ResponseEntity.ok(DtoMapper.toResponse(policy));
    }
    
    @PostMapping("/{id}/publish")
    public ResponseEntity<PolicyResponse> publishPolicy(@PathVariable String id) {
        PolicyDefinition policy = policyService.publish(new PolicyId(id));
        return ResponseEntity.ok(DtoMapper.toResponse(policy));
    }
}
