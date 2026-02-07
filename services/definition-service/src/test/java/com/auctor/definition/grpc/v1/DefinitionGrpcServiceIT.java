package com.auctor.definition.grpc.v1;

import com.auctor.definition.IntegrationTestBase;
import com.auctor.definition.domain.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DefinitionGrpcService.
 * Note: Full gRPC testing requires more setup with in-process server.
 * This is a basic smoke test to verify the service is wired correctly.
 */
class DefinitionGrpcServiceIT extends IntegrationTestBase {
    
    @Autowired
    private DefinitionGrpcService grpcService;
    
    @Autowired
    private WorkflowService workflowService;
    
    @Test
    void shouldWireGrpcService() {
        assertNotNull(grpcService);
        assertNotNull(workflowService);
    }
    
    // Additional tests would require setting up grpc-testing infrastructure
    // with ManagedChannel and in-process server for full end-to-end gRPC testing
}
