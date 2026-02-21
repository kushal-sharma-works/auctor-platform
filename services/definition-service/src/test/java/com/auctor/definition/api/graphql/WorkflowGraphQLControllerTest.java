package com.auctor.definition.api.graphql;

import com.auctor.definition.api.graphql.dto.WorkflowGraphQLDto;
import com.auctor.definition.api.graphql.input.CreateWorkflowInput;
import com.auctor.definition.api.graphql.input.TransitionInput;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.domain.service.WorkflowService;
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
class WorkflowGraphQLControllerTest {

    @Mock
    private WorkflowService workflowService;

    private WorkflowGraphQLController controller;

    @BeforeEach
    void setUp() {
        controller = new WorkflowGraphQLController(workflowService);
    }

    @Test
    void shouldReturnWorkflowById() {
        WorkflowDefinition workflow = buildWorkflow("wf-1");
        when(workflowService.getById(eq(new WorkflowId("wf-1")))).thenReturn(workflow);

        WorkflowGraphQLDto dto = controller.workflow("wf-1");

        assertEquals("wf-1", dto.id());
        assertEquals("Workflow", dto.name());
        verify(workflowService).getById(eq(new WorkflowId("wf-1")));
    }

    @Test
    void shouldUseDefaultPaginationWhenNull() {
        when(workflowService.listAll(any())).thenReturn(new PageImpl<>(List.of(buildWorkflow("wf-1"))));

        controller.workflows(null, null);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(workflowService).listAll(captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(20, captor.getValue().getPageSize());
    }

    @Test
    void shouldCreateWorkflowFromInput() {
        WorkflowDefinition workflow = buildWorkflow("wf-2");
        when(workflowService.create(any(), any(), any(), any())).thenReturn(workflow);

        CreateWorkflowInput input = new CreateWorkflowInput(
            "Workflow",
            List.of("START", "END"),
            "START",
            List.of(new TransitionInput("START", "END", "policy-1"))
        );

        WorkflowGraphQLDto dto = controller.createWorkflow(input);

        assertEquals("wf-2", dto.id());
        ArgumentCaptor<List<Transition>> transitionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(workflowService).create(eq("Workflow"), eq(List.of("START", "END")), eq("START"), transitionsCaptor.capture());
        assertEquals(1, transitionsCaptor.getValue().size());
        assertEquals("policy-1", transitionsCaptor.getValue().get(0).policyRef());
    }

    @Test
    void shouldPublishWorkflow() {
        WorkflowDefinition workflow = buildWorkflow("wf-3");
        when(workflowService.publish(eq(new WorkflowId("wf-3")))).thenReturn(workflow);

        WorkflowGraphQLDto dto = controller.publishWorkflow("wf-3");

        assertEquals("wf-3", dto.id());
        verify(workflowService).publish(eq(new WorkflowId("wf-3")));
    }

    private WorkflowDefinition buildWorkflow(String id) {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        return new WorkflowDefinition(
            new WorkflowId(id),
            "Workflow",
            1,
            new WorkflowStatus.Draft(),
            List.of("START", "END"),
            "START",
            List.of(new Transition("START", "END", "policy-1", null)),
            now,
            now
        );
    }
}
