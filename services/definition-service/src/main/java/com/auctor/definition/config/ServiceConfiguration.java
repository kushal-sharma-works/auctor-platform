package com.auctor.definition.config;

import com.auctor.definition.domain.port.*;
import com.auctor.definition.domain.service.PolicyEvaluator;
import com.auctor.definition.domain.service.PolicyService;
import com.auctor.definition.domain.service.WorkflowService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for domain services.
 * Wires up domain services with their dependencies.
 */
@Configuration
public class ServiceConfiguration {
    
    @Bean
    public WorkflowService workflowService(
        WorkflowCommandPort commandPort,
        WorkflowQueryPort queryPort,
        MeterRegistry meterRegistry
    ) {
        return new WorkflowService(commandPort, queryPort, meterRegistry);
    }
    
    @Bean
    public PolicyService policyService(
        PolicyCommandPort commandPort,
        PolicyQueryPort queryPort
    ) {
        return new PolicyService(commandPort, queryPort);
    }
    
    @Bean
    public PolicyEvaluator policyEvaluator(MeterRegistry meterRegistry) {
        return new PolicyEvaluator(meterRegistry);
    }
}
