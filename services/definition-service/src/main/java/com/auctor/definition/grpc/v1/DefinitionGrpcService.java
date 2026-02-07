package com.auctor.definition.grpc.v1;

import com.auctor.definition.domain.exception.EntityNotFoundException;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.domain.service.PolicyEvaluator;
import com.auctor.definition.domain.service.PolicyService;
import com.auctor.definition.domain.service.WorkflowService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * gRPC service implementation for workflow and policy definitions.
 */
@GrpcService
public class DefinitionGrpcService extends DefinitionServiceGrpc.DefinitionServiceImplBase {
    
    private static final Logger logger = LoggerFactory.getLogger(DefinitionGrpcService.class);
    
    private final WorkflowService workflowService;
    private final PolicyService policyService;
    private final PolicyEvaluator policyEvaluator;
    
    public DefinitionGrpcService(
        WorkflowService workflowService,
        PolicyService policyService,
        PolicyEvaluator policyEvaluator
    ) {
        this.workflowService = workflowService;
        this.policyService = policyService;
        this.policyEvaluator = policyEvaluator;
    }
    
    @Override
    public void getWorkflow(GetWorkflowRequest request, StreamObserver<WorkflowResponse> responseObserver) {
        try {
            WorkflowId id = new WorkflowId(request.getId());
            WorkflowDefinition workflow;
            
            if (request.getVersion() > 0) {
                workflow = workflowService.getByIdAndVersion(id, request.getVersion());
            } else {
                workflow = workflowService.getById(id);
            }
            
            WorkflowResponse.Builder responseBuilder = WorkflowResponse.newBuilder()
                .setId(workflow.id().value())
                .setName(workflow.name())
                .setVersion(workflow.version())
                .setStatus(workflow.status().label())
                .addAllStates(workflow.states())
                .setInitialState(workflow.initialState());
            
            for (Transition transition : workflow.transitions()) {
                TransitionProto.Builder transitionBuilder = TransitionProto.newBuilder()
                    .setFromState(transition.fromState())
                    .setToState(transition.toState());
                
                if (transition.policyRef() != null) {
                    transitionBuilder.setPolicyRef(transition.policyRef());
                }
                
                responseBuilder.addTransitions(transitionBuilder.build());
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (EntityNotFoundException e) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription("Workflow not found").asException()
            );
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid argument in getWorkflow: {}", e.getMessage());
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription("Invalid request").asException()
            );
        } catch (Exception e) {
            logger.error("Internal error in getWorkflow", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asException()
            );
        }
    }
    
    @Override
    public void getPolicy(GetPolicyRequest request, StreamObserver<PolicyResponse> responseObserver) {
        try {
            PolicyId id = new PolicyId(request.getId());
            PolicyDefinition policy;
            
            if (request.getVersion() > 0) {
                policy = policyService.getByIdAndVersion(id, request.getVersion());
            } else {
                policy = policyService.getById(id);
            }
            
            PolicyResponse.Builder responseBuilder = PolicyResponse.newBuilder()
                .setId(policy.id().value())
                .setName(policy.name())
                .setVersion(policy.version())
                .setStatus(policy.status().label());
            
            for (PolicyCondition condition : policy.conditions()) {
                PolicyConditionProto conditionProto = PolicyConditionProto.newBuilder()
                    .setField(condition.field())
                    .setOperator(condition.operator().name())
                    .setValue(condition.value())
                    .build();
                responseBuilder.addConditions(conditionProto);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (EntityNotFoundException e) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription("Policy not found").asException()
            );
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid argument in getPolicy: {}", e.getMessage());
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription("Invalid request").asException()
            );
        } catch (Exception e) {
            logger.error("Internal error in getPolicy", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asException()
            );
        }
    }
    
    @Override
    public void evaluatePolicy(EvaluatePolicyRequest request, StreamObserver<EvaluatePolicyResponse> responseObserver) {
        try {
            PolicyId id = new PolicyId(request.getPolicyId());
            PolicyDefinition policy;
            
            if (request.getPolicyVersion() > 0) {
                policy = policyService.getByIdAndVersion(id, request.getPolicyVersion());
            } else {
                policy = policyService.getById(id);
            }
            
            Map<String, Object> context = new HashMap<>(request.getContextMap());
            EvaluationResult result = policyEvaluator.evaluate(policy, context);
            
            EvaluatePolicyResponse response = EvaluatePolicyResponse.newBuilder()
                .setAllowed(result.allowed())
                .setExplanation(result.explanation())
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (EntityNotFoundException e) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription("Policy not found").asException()
            );
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid argument in evaluatePolicy: {}", e.getMessage());
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription("Invalid request").asException()
            );
        } catch (Exception e) {
            logger.error("Internal error in evaluatePolicy", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asException()
            );
        }
    }
}
