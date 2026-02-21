package com.auctor.definition.api.graphql;

import com.auctor.definition.domain.exception.EntityNotFoundException;
import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphQLExceptionHandlerTest {

    private GraphQLExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GraphQLExceptionHandler();
    }

    @Test
    void shouldMapEntityNotFoundToGraphQLError() {
        DataFetchingEnvironment env = mockEnv();

        GraphQLError error = handler.resolveToSingleError(
            new EntityNotFoundException("Workflow", "wf-1"),
            env
        );

        assertNotNull(error);
        assertEquals("Workflow not found with id: wf-1", error.getMessage());
        assertEquals("NOT_FOUND", error.getErrorType().toString());
    }

    @Test
    void shouldMapIllegalArgumentToGraphQLError() {
        DataFetchingEnvironment env = mockEnv();

        GraphQLError error = handler.resolveToSingleError(
            new IllegalArgumentException("invalid"),
            env
        );

        assertNotNull(error);
        assertEquals("invalid", error.getMessage());
        assertEquals("BAD_REQUEST", error.getErrorType().toString());
    }

    @Test
    void shouldReturnNullForUnhandledExceptions() {
        DataFetchingEnvironment env = mockEnv();

        GraphQLError error = handler.resolveToSingleError(new RuntimeException("boom"), env);

        assertNull(error);
    }

    private DataFetchingEnvironment mockEnv() {
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        ExecutionStepInfo stepInfo = ExecutionStepInfo.newExecutionStepInfo()
            .path(ResultPath.rootPath().segment("workflow"))
            .type(GraphQLObjectType.newObject().name("Query").build())
            .build();
        Field field = Field.newField("workflow")
            .sourceLocation(new SourceLocation(1, 1))
            .build();

        when(env.getExecutionStepInfo()).thenReturn(stepInfo);
        when(env.getField()).thenReturn(field);
        return env;
    }
}
