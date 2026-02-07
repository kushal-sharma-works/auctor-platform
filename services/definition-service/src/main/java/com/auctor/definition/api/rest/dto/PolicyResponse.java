package com.auctor.definition.api.rest.dto;

import java.time.Instant;
import java.util.List;

/**
 * Policy response DTO.
 */
public record PolicyResponse(
    String id,
    String name,
    int version,
    String status,
    List<PolicyConditionResponse> conditions,
    Instant createdAt
) {
}
