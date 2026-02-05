package com.auctor.definition.grpc.v1;

import com.auctor.definition.grpc.v1.DefinitionServiceGrpc;
import com.auctor.definition.grpc.v1.GetDefinitionRequest;
import com.auctor.definition.grpc.v1.GetDefinitionResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;


@GrpcService
public class DefinitionGrpcService
        extends DefinitionServiceGrpc.DefinitionServiceImplBase {

    @Override
    public void getDefinition(
            GetDefinitionRequest request,
            StreamObserver<GetDefinitionResponse> responseObserver
    ) {
        GetDefinitionResponse response =
                GetDefinitionResponse.newBuilder()
                        .setId(request.getId())
                        .setName("sample-definition")
                        .setDescription("mock definition response")
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
