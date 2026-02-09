package com.auctor.definition.api.graphql;

import com.auctor.definition.domain.exception.EntityNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * GraphQL error handler.
 * Maps domain exceptions to GraphQL errors with proper error types and extensions.
 */
@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {
    
    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof EntityNotFoundException notFound) {
            return GraphqlErrorBuilder.newError()
                .errorType(ErrorType.NOT_FOUND)
                .message(notFound.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
        }
        
        if (ex instanceof IllegalArgumentException illegalArg) {
            return GraphqlErrorBuilder.newError()
                .errorType(ErrorType.BAD_REQUEST)
                .message(illegalArg.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
        }
        
        return null; // Let default handler handle other exceptions
    }
}
