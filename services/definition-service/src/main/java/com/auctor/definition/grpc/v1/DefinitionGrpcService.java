package com.auctor.definition.grpc.v1;

import com.auctor.definition.domain.model.DefinitionId;
import com.auctor.definition.domain.service.DefinitionService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class DefinitionGrpcService
        extends DefinitionServiceGrpc.DefinitionServiceImplBase {

    private final DefinitionService definitionService;

    public DefinitionGrpcService(DefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @Override
    public void getDefinition(
            GetDefinitionRequest request,
            StreamObserver<GetDefinitionResponse> responseObserver
    ) {
        var definition = definitionService.getDefinition(
        new DefinitionId(request.getId())
        );

        responseObserver.onNext(
                GetDefinitionResponse.newBuilder()
                        .setId(definition.id().value())
                        .setName(definition.name())
                        .setDescription(definition.description())
                        .build()
        );
        responseObserver.onCompleted();
    }
}
