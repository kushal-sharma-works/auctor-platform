package com.auctor.definition.api.graphql;

import com.auctor.definition.api.graphql.dto.PolicyGraphQLDto;
import com.auctor.definition.api.graphql.input.CreatePolicyInput;
import com.auctor.definition.api.graphql.input.PolicyConditionInput;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.domain.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyGraphQLControllerTest {

    @Mock
    private PolicyService policyService;

    private PolicyGraphQLController controller;

    @BeforeEach
    void setUp() {
        controller = new PolicyGraphQLController(policyService);
    }

    @Test
    void shouldReturnPolicyById() {
        PolicyDefinition policy = buildPolicy("policy-1");
        when(policyService.getById(eq(new PolicyId("policy-1")))).thenReturn(policy);

        PolicyGraphQLDto dto = controller.policy("policy-1");

        assertEquals("policy-1", dto.id());
        assertEquals("Policy", dto.name());
        verify(policyService).getById(eq(new PolicyId("policy-1")));
    }

    @Test
    void shouldUseDefaultPaginationWhenNull() {
        when(policyService.listAll(any())).thenReturn(new PageImpl<>(List.of(buildPolicy("policy-1"))));

        controller.policies(null, null);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(policyService).listAll(captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(20, captor.getValue().getPageSize());
    }

    @Test
    void shouldCreatePolicyFromInput() {
        PolicyDefinition policy = buildPolicy("policy-2");
        when(policyService.create(any(), any())).thenReturn(policy);

        CreatePolicyInput input = new CreatePolicyInput(
            "Policy",
            List.of(new PolicyConditionInput("amount", "GTE", "100"))
        );

        PolicyGraphQLDto dto = controller.createPolicy(input);

        assertEquals("policy-2", dto.id());
        ArgumentCaptor<List<PolicyCondition>> conditionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(policyService).create(eq("Policy"), conditionsCaptor.capture());
        assertEquals(1, conditionsCaptor.getValue().size());
        assertEquals(Operator.GTE, conditionsCaptor.getValue().get(0).operator());
    }

    @Test
    void shouldPublishPolicy() {
        PolicyDefinition policy = buildPolicy("policy-3");
        when(policyService.publish(eq(new PolicyId("policy-3")))).thenReturn(policy);

        PolicyGraphQLDto dto = controller.publishPolicy("policy-3");

        assertEquals("policy-3", dto.id());
        verify(policyService).publish(eq(new PolicyId("policy-3")));
    }

    private PolicyDefinition buildPolicy(String id) {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        return new PolicyDefinition(
            new PolicyId(id),
            "Policy",
            1,
            new PolicyStatus.Draft(),
            List.of(new PolicyCondition("amount", Operator.GTE, "100")),
            now
        );
    }
}
