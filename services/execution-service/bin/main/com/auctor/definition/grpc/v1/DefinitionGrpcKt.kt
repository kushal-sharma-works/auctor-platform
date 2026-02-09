package com.auctor.definition.grpc.v1

import com.auctor.definition.grpc.v1.DefinitionServiceGrpc.getServiceDescriptor
import io.grpc.CallOptions
import io.grpc.CallOptions.DEFAULT
import io.grpc.Channel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServerServiceDefinition.builder
import io.grpc.ServiceDescriptor
import io.grpc.Status.UNIMPLEMENTED
import io.grpc.StatusException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls.unaryRpc
import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition
import io.grpc.kotlin.StubFor
import kotlin.String
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Holder for Kotlin coroutine-based client and server APIs for definition.v1.DefinitionService.
 */
public object DefinitionServiceGrpcKt {
  public const val SERVICE_NAME: String = DefinitionServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = getServiceDescriptor()

  public val getWorkflowMethod: MethodDescriptor<GetWorkflowRequest, WorkflowResponse>
    @JvmStatic
    get() = DefinitionServiceGrpc.getGetWorkflowMethod()

  public val getPolicyMethod: MethodDescriptor<GetPolicyRequest, PolicyResponse>
    @JvmStatic
    get() = DefinitionServiceGrpc.getGetPolicyMethod()

  public val evaluatePolicyMethod: MethodDescriptor<EvaluatePolicyRequest, EvaluatePolicyResponse>
    @JvmStatic
    get() = DefinitionServiceGrpc.getEvaluatePolicyMethod()

  /**
   * A stub for issuing RPCs to a(n) definition.v1.DefinitionService service as suspending
   * coroutines.
   */
  @StubFor(DefinitionServiceGrpc::class)
  public class DefinitionServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<DefinitionServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): DefinitionServiceCoroutineStub =
        DefinitionServiceCoroutineStub(channel, callOptions)

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun getWorkflow(request: GetWorkflowRequest, headers: Metadata = Metadata()):
        WorkflowResponse = unaryRpc(
      channel,
      DefinitionServiceGrpc.getGetWorkflowMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun getPolicy(request: GetPolicyRequest, headers: Metadata = Metadata()):
        PolicyResponse = unaryRpc(
      channel,
      DefinitionServiceGrpc.getGetPolicyMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun evaluatePolicy(request: EvaluatePolicyRequest, headers: Metadata =
        Metadata()): EvaluatePolicyResponse = unaryRpc(
      channel,
      DefinitionServiceGrpc.getEvaluatePolicyMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the definition.v1.DefinitionService service based on Kotlin
   * coroutines.
   */
  public abstract class DefinitionServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for definition.v1.DefinitionService.GetWorkflow.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getWorkflow(request: GetWorkflowRequest): WorkflowResponse = throw
        StatusException(UNIMPLEMENTED.withDescription("Method definition.v1.DefinitionService.GetWorkflow is unimplemented"))

    /**
     * Returns the response to an RPC for definition.v1.DefinitionService.GetPolicy.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getPolicy(request: GetPolicyRequest): PolicyResponse = throw
        StatusException(UNIMPLEMENTED.withDescription("Method definition.v1.DefinitionService.GetPolicy is unimplemented"))

    /**
     * Returns the response to an RPC for definition.v1.DefinitionService.EvaluatePolicy.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun evaluatePolicy(request: EvaluatePolicyRequest): EvaluatePolicyResponse =
        throw
        StatusException(UNIMPLEMENTED.withDescription("Method definition.v1.DefinitionService.EvaluatePolicy is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = DefinitionServiceGrpc.getGetWorkflowMethod(),
      implementation = ::getWorkflow
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = DefinitionServiceGrpc.getGetPolicyMethod(),
      implementation = ::getPolicy
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = DefinitionServiceGrpc.getEvaluatePolicyMethod(),
      implementation = ::evaluatePolicy
    )).build()
  }
}
