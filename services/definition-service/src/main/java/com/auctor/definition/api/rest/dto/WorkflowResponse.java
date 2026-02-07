package com.auctor.definition.api.rest.dto;

import java.time.Instant;
import java.util.List;

/**
 * Workflow response DTO.
 */
public record WorkflowResponse(
    String id,
    String name,
    int version,
    String status,
    List<String> states,
    String initialState,
    List<TransitionResponse> transitions,
    Instant createdAt,
    Instant updatedAt
) {
}
