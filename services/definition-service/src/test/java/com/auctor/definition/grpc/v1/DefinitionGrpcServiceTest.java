package com.auctor.definition.grpc.v1;

import com.auctor.definition.domain.exception.EntityNotFoundException;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.domain.service.PolicyEvaluator;
import com.auctor.definition.domain.service.PolicyService;
import com.auctor.definition.domain.service.WorkflowService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DefinitionGrpcService.
 * Tests gRPC method behavior with mocked service dependencies.
 */
@ExtendWith(MockitoExtension.class)
class DefinitionGrpcServiceTest {
    
    @Mock
    private WorkflowService workflowService;
    
    @Mock
    private PolicyService policyService;
    
    @Mock
    private PolicyEvaluator policyEvaluator;
    
    @Mock
    private StreamObserver<WorkflowResponse> workflowObserver;
    
    @Mock
    private StreamObserver<PolicyResponse> policyObserver;
    
    @Mock
    private StreamObserver<EvaluatePolicyResponse> evaluateObserver;
    
    @Captor
    private ArgumentCaptor<WorkflowResponse> workflowCaptor;
    
    @Captor
    private ArgumentCaptor<PolicyResponse> policyCaptor;
    
    @Captor
    private ArgumentCaptor<EvaluatePolicyResponse> evaluateCaptor;
    
    @Captor
    private ArgumentCaptor<Throwable> errorCaptor;
    
    private DefinitionGrpcService service;
    
    @BeforeEach
    void setUp() {
        service = new DefinitionGrpcService(workflowService, policyService, policyEvaluator);
    }
    
    // ========== GetWorkflow Tests ==========
    
    @Test
    void shouldGetWorkflowByLatestVersion() {
        // Given
        WorkflowId id = new WorkflowId("wf-1");
        WorkflowDefinition workflow = createWorkflow(id, 1);
        when(workflowService.getById(id)).thenReturn(workflow);
        
        GetWorkflowRequest request = GetWorkflowRequest.newBuilder()
            .setId("wf-1")
            .build();
        
        // When
        service.getWorkflow(request, workflowObserver);
        
        // Then
        verify(workflowService).getById(id);
        verify(workflowObserver).onNext(workflowCaptor.capture());
        verify(workflowObserver).onCompleted();
        
        WorkflowResponse response = workflowCaptor.getValue();
        assertEquals("wf-1", response.getId());
        assertEquals("Test Workflow", response.getName());
        assertEquals(1, response.getVersion());
    }
    
    @Test
    void shouldGetWorkflowBySpecificVersion() {
        // Given
        WorkflowId id = new WorkflowId("wf-1");
        WorkflowDefinition workflow = createWorkflow(id, 2);
        when(workflowService.getByIdAndVersion(id, 2)).thenReturn(workflow);
        
        GetWorkflowRequest request = GetWorkflowRequest.newBuilder()
            .setId("wf-1")
            .setVersion(2)
            .build();
        
        // When
        service.getWorkflow(request, workflowObserver);
        
        // Then
        verify(workflowService).getByIdAndVersion(id, 2);
        verify(workflowObserver).onNext(workflowCaptor.capture());
        assertEquals(2, workflowCaptor.getValue().getVersion());
    }
    
    @Test
    void shouldReturnNotFoundForMissingWorkflow() {
        // Given
        WorkflowId id = new WorkflowId("non-existent");
        when(workflowService.getById(id))
            .thenThrow(new EntityNotFoundException("Workflow", "non-existent"));
        
        GetWorkflowRequest request = GetWorkflowRequest.newBuilder()
            .setId("non-existent")
            .build();
        
        // When
        service.getWorkflow(request, workflowObserver);
        
        // Then
        verify(workflowObserver).onError(errorCaptor.capture());
        Throwable error = errorCaptor.getValue();
        assertInstanceOf(io.grpc.StatusException.class, error);
        assertEquals(Status.Code.NOT_FOUND, ((io.grpc.StatusException) error).getStatus().getCode());
    }
    
    @Test
    void shouldMapWorkflowStates() {
        // Given
        WorkflowId id = new WorkflowId("wf-1");
        WorkflowDefinition workflow = createWorkflowWithStates(id, List.of("DRAFT", "REVIEW", "APPROVED"));
        when(workflowService.getById(id)).thenReturn(workflow);
        
        GetWorkflowRequest request = GetWorkflowRequest.newBuilder()
            .setId("wf-1")
            .build();
        
        // When
        service.getWorkflow(request, workflowObserver);
        
        // Then
        verify(workflowObserver).onNext(workflowCaptor.capture());
        WorkflowResponse response = workflowCaptor.getValue();
        assertEquals(3, response.getStatesList().size());
        assertTrue(response.getStatesList().containsAll(List.of("DRAFT", "REVIEW", "APPROVED")));
    }
    
    @Test
    void shouldMapWorkflowTransitions() {
        // Given
        WorkflowId id = new WorkflowId("wf-1");
        List<Transition> transitions = List.of(
            new Transition("DRAFT", "REVIEW", null, null),
            new Transition("REVIEW", "APPROVED", "approval-policy", "isApprover == true")
        );
        WorkflowDefinition workflow = createWorkflowWithTransitions(id, transitions);
        when(workflowService.getById(id)).thenReturn(workflow);
        
        GetWorkflowRequest request = GetWorkflowRequest.newBuilder()
            .setId("wf-1")
            .build();
        
        // When
        service.getWorkflow(request, workflowObserver);
        
        // Then
        verify(workflowObserver).onNext(workflowCaptor.capture());
        WorkflowResponse response = workflowCaptor.getValue();
        assertEquals(2, response.getTransitionsCount());
        assertEquals("DRAFT", response.getTransitions(0).getFromState());
        assertEquals("REVIEW", response.getTransitions(0).getToState());
        assertEquals("approval-policy", response.getTransitions(1).getPolicyRef());
    }
    
    // ========== GetPolicy Tests ==========
    
    @Test
    void shouldGetPolicyByLatestVersion() {
        // Given
        PolicyId id = new PolicyId("pol-1");
        PolicyDefinition policy = createPolicy(id, 1);
        when(policyService.getById(id)).thenReturn(policy);
        
        GetPolicyRequest request = GetPolicyRequest.newBuilder()
            .setId("pol-1")
            .build();
        
        // When
        service.getPolicy(request, policyObserver);
        
        // Then
        verify(policyService).getById(id);
        verify(policyObserver).onNext(policyCaptor.capture());
        verify(policyObserver).onCompleted();
        
        PolicyResponse response = policyCaptor.getValue();
        assertEquals("pol-1", response.getId());
        assertEquals("Test Policy", response.getName());
        assertEquals(1, response.getVersion());
    }
    
    @Test
    void shouldGetPolicyBySpecificVersion() {
        // Given
        PolicyId id = new PolicyId("pol-1");
        PolicyDefinition policy = createPolicy(id, 3);
        when(policyService.getByIdAndVersion(id, 3)).thenReturn(policy);
        
        GetPolicyRequest request = GetPolicyRequest.newBuilder()
            .setId("pol-1")
            .setVersion(3)
            .build();
        
        // When
        service.getPolicy(request, policyObserver);
        
        // Then
        verify(policyService).getByIdAndVersion(id, 3);
        verify(policyObserver).onNext(policyCaptor.capture());
        assertEquals(3, policyCaptor.getValue().getVersion());
    }
    
    @Test
    void shouldReturnNotFoundForMissingPolicy() {
        // Given
        PolicyId id = new PolicyId("non-existent");
        when(policyService.getById(id))
            .thenThrow(new EntityNotFoundException("Policy", "non-existent"));
        
        GetPolicyRequest request = GetPolicyRequest.newBuilder()
            .setId("non-existent")
            .build();
        
        // When
        service.getPolicy(request, policyObserver);
        
        // Then
        verify(policyObserver).onError(errorCaptor.capture());
        Throwable error = errorCaptor.getValue();
        assertInstanceOf(io.grpc.StatusException.class, error);
        assertEquals(Status.Code.NOT_FOUND, ((io.grpc.StatusException) error).getStatus().getCode());
    }
    
    @Test
    void shouldMapPolicyConditions() {
        // Given
        PolicyId id = new PolicyId("pol-1");
        List<PolicyCondition> conditions = List.of(
            new PolicyCondition("amount", Operator.LTE, "1000"),
            new PolicyCondition("status", Operator.EQ, "ACTIVE"),
            new PolicyCondition("region", Operator.IN, "US,EU,APAC")
        );
        PolicyDefinition policy = new PolicyDefinition(
            id, "Test", 1, new PolicyStatus.Draft(), conditions, Instant.now()
        );
        when(policyService.getById(id)).thenReturn(policy);
        
        GetPolicyRequest request = GetPolicyRequest.newBuilder()
            .setId("pol-1")
            .build();
        
        // When
        service.getPolicy(request, policyObserver);
        
        // Then
        verify(policyObserver).onNext(policyCaptor.capture());
        PolicyResponse response = policyCaptor.getValue();
        assertEquals(3, response.getConditionsCount());
        
        assertEquals("amount", response.getConditions(0).getField());
        assertEquals("LTE", response.getConditions(0).getOperator());
        
        assertEquals("status", response.getConditions(1).getField());
        assertEquals("EQ", response.getConditions(1).getOperator());
        
        assertEquals("region", response.getConditions(2).getField());
        assertEquals("IN", response.getConditions(2).getOperator());
    }
    
    // ========== EvaluatePolicy Tests ==========
    
    @Test
    void shouldEvaluatePolicyLatestVersion() {
        // Given
        PolicyId id = new PolicyId("pol-1");
        PolicyDefinition policy = createPolicy(id, 1);
        EvaluationResult result = new EvaluationResult(true, "Amount within limit");
        
        when(policyService.getById(id)).thenReturn(policy);
        when(policyEvaluator.evaluate(eq(policy), any())).thenReturn(result);
        
        EvaluatePolicyRequest request = EvaluatePolicyRequest.newBuilder()
            .setPolicyId("pol-1")
            .putAllContext(java.util.Map.of("amount", "500"))
            .build();
        
        // When
        service.evaluatePolicy(request, evaluateObserver);
        
        // Then
        verify(policyService).getById(id);
        verify(policyEvaluator).evaluate(eq(policy), any());
        verify(evaluateObserver).onNext(evaluateCaptor.capture());
        verify(evaluateObserver).onCompleted();
        
        EvaluatePolicyResponse response = evaluateCaptor.getValue();
        assertTrue(response.getAllowed());
        assertEquals("Amount within limit", response.getExplanation());
    }
    
    @Test
    void shouldEvaluatePolicySpecificVersion() {
        // Given
        PolicyId id = new PolicyId("pol-1");
        PolicyDefinition policy = createPolicy(id, 2);
        EvaluationResult result = new EvaluationResult(true, "OK");
        
        when(policyService.getByIdAndVersion(id, 2)).thenReturn(policy);
        when(policyEvaluator.evaluate(eq(policy), any())).thenReturn(result);
        
        EvaluatePolicyRequest request = EvaluatePolicyRequest.newBuilder()
            .setPolicyId("pol-1")
            .setPolicyVersion(2)
            .putAllContext(java.util.Map.of("amount", "500"))
            .build();
        
        // When
        service.evaluatePolicy(request, evaluateObserver);
        
        // Then
        verify(policyService).getByIdAndVersion(id, 2);
        verify(evaluateObserver).onNext(evaluateCaptor.capture());
    }
    
    @Test
    void shouldEvaluatePolicyAllowed() {
        // Given
        PolicyId id = new PolicyId("pol-1");
        PolicyDefinition policy = createPolicy(id, 1);
        EvaluationResult result = new EvaluationResult(true, "Conditions satisfied");
        
        when(policyService.getById(id)).thenReturn(policy);
        when(policyEvaluator.evaluate(eq(policy), any())).thenReturn(result);
        
        EvaluatePolicyRequest request = EvaluatePolicyRequest.newBuilder()
            .setPolicyId("pol-1")
            .putAllContext(java.util.Map.of("amount", "500"))
            .build();
        
        // When
        service.evaluatePolicy(request, evaluateObserver);
        
        // Then
        verify(evaluateObserver).onNext(evaluateCaptor.capture());
        EvaluatePolicyResponse response = evaluateCaptor.getValue();
        assertTrue(response.getAllowed());
    }
    
    @Test
    void shouldEvaluatePolicyDenied() {
        // Given
        PolicyId id = new PolicyId("pol-1");
        PolicyDefinition policy = createPolicy(id, 1);
        EvaluationResult result = new EvaluationResult(false, "Amount exceeds limit");
        
        when(policyService.getById(id)).thenReturn(policy);
        when(policyEvaluator.evaluate(eq(policy), any())).thenReturn(result);
        
        EvaluatePolicyRequest request = EvaluatePolicyRequest.newBuilder()
            .setPolicyId("pol-1")
            .putAllContext(java.util.Map.of("amount", "5000"))
            .build();
        
        // When
        service.evaluatePolicy(request, evaluateObserver);
        
        // Then
        verify(evaluateObserver).onNext(evaluateCaptor.capture());
        EvaluatePolicyResponse response = evaluateCaptor.getValue();
        assertFalse(response.getAllowed());
        assertEquals("Amount exceeds limit", response.getExplanation());
    }
    
    @Test
    void shouldReturnNotFoundWhenEvaluatingNonExistentPolicy() {
        // Given
        PolicyId id = new PolicyId("non-existent");
        when(policyService.getById(id))
            .thenThrow(new EntityNotFoundException("Policy", "non-existent"));
        
        EvaluatePolicyRequest request = EvaluatePolicyRequest.newBuilder()
            .setPolicyId("non-existent")
            .build();
        
        // When
        service.evaluatePolicy(request, evaluateObserver);
        
        // Then
        verify(evaluateObserver).onError(errorCaptor.capture());
        Throwable error = errorCaptor.getValue();
        assertEquals(Status.Code.NOT_FOUND, ((io.grpc.StatusException) error).getStatus().getCode());
    }
    
    // ========== Error Handling Tests ==========
    
    @Test
    void shouldHandleInternalErrorInGetWorkflow() {
        // Given
        when(workflowService.getById(any()))
            .thenThrow(new RuntimeException("Database error"));
        
        GetWorkflowRequest request = GetWorkflowRequest.newBuilder()
            .setId("wf-1")
            .build();
        
        // When
        service.getWorkflow(request, workflowObserver);
        
        // Then
        verify(workflowObserver).onError(errorCaptor.capture());
        Throwable error = errorCaptor.getValue();
        assertEquals(Status.Code.INTERNAL, ((io.grpc.StatusException) error).getStatus().getCode());
    }
    
    @Test
    void shouldHandleInternalErrorInGetPolicy() {
        // Given
        when(policyService.getById(any()))
            .thenThrow(new RuntimeException("Service unavailable"));
        
        GetPolicyRequest request = GetPolicyRequest.newBuilder()
            .setId("pol-1")
            .build();
        
        // When
        service.getPolicy(request, policyObserver);
        
        // Then
        verify(policyObserver).onError(errorCaptor.capture());
        Throwable error = errorCaptor.getValue();
        assertEquals(Status.Code.INTERNAL, ((io.grpc.StatusException) error).getStatus().getCode());
    }
    
    @Test
    void shouldHandleInternalErrorInEvaluatePolicy() {
        // Given
        PolicyId id = new PolicyId("pol-1");
        PolicyDefinition policy = createPolicy(id, 1);
        when(policyService.getById(id)).thenReturn(policy);
        when(policyEvaluator.evaluate(any(), any()))
            .thenThrow(new RuntimeException("Evaluation failed"));
        
        EvaluatePolicyRequest request = EvaluatePolicyRequest.newBuilder()
            .setPolicyId("pol-1")
            .build();
        
        // When
        service.evaluatePolicy(request, evaluateObserver);
        
        // Then
        verify(evaluateObserver).onError(errorCaptor.capture());
        Throwable error = errorCaptor.getValue();
        assertEquals(Status.Code.INTERNAL, ((io.grpc.StatusException) error).getStatus().getCode());
    }
    
    // ========== Helper Methods ==========
    
    private WorkflowDefinition createWorkflow(WorkflowId id, int version) {
        return new WorkflowDefinition(
            id, "Test Workflow", version, new WorkflowStatus.Draft(),
            List.of("START", "END"), "START",
            List.of(new Transition("START", "END", null, null)),
            Instant.now(), Instant.now()
        );
    }
    
    private WorkflowDefinition createWorkflowWithStates(WorkflowId id, List<String> states) {
        return new WorkflowDefinition(
            id, "Test Workflow", 1, new WorkflowStatus.Draft(),
            states, states.get(0),
            List.of(new Transition(states.get(0), states.get(states.size() - 1), null, null)),
            Instant.now(), Instant.now()
        );
    }
    
    private WorkflowDefinition createWorkflowWithTransitions(WorkflowId id, List<Transition> transitions) {
        return new WorkflowDefinition(
            id, "Test Workflow", 1, new WorkflowStatus.Draft(),
            List.of("DRAFT", "REVIEW", "APPROVED"), "DRAFT",
            transitions,
            Instant.now(), Instant.now()
        );
    }
    
    private PolicyDefinition createPolicy(PolicyId id, int version) {
        return new PolicyDefinition(
            id, "Test Policy", version, new PolicyStatus.Draft(),
            List.of(new PolicyCondition("amount", Operator.LTE, "1000")),
            Instant.now()
        );
    }
}
