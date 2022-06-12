package io.andrewohara.awsmock.iot

import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient
import software.amazon.awssdk.services.iotdataplane.model.*

class MockIotDataV2(private val backend: MockIotDataBackend = MockIotDataBackend()): IotDataPlaneClient {
    override fun close() {}
    override fun serviceName() = "iotdata-mock"

    override fun deleteThingShadow(request: DeleteThingShadowRequest): DeleteThingShadowResponse {
        val payload = backend.delete(request.thingName(), request.shadowName())
            ?: throw shadowNotFound(request.thingName(), request.shadowName())

        return DeleteThingShadowResponse.builder()
            .payload(SdkBytes.fromByteBuffer(payload))
            .build()
    }

    override fun updateThingShadow(request: UpdateThingShadowRequest): UpdateThingShadowResponse {
       backend[request.thingName(), request.shadowName()] = request.payload().asByteBuffer()

        return UpdateThingShadowResponse.builder()
            .payload(request.payload())
            .build()
    }

    override fun getThingShadow(request: GetThingShadowRequest): GetThingShadowResponse {
        val payload = backend[request.thingName(), request.shadowName()]
            ?: throw shadowNotFound(request.thingName(), request.shadowName())

        return GetThingShadowResponse.builder()
            .payload(SdkBytes.fromByteBuffer(payload))
            .build()
    }

    override fun listNamedShadowsForThing(request: ListNamedShadowsForThingRequest): ListNamedShadowsForThingResponse {
        val shadowNames = backend.listNamedShadows(request.thingName())

        return ListNamedShadowsForThingResponse.builder()
            .results(shadowNames ?: emptyList())
            .build()
    }

    private fun shadowNotFound(thingName: String, shadowName: String?): ResourceNotFoundException {
        val message = "No shadow exists with name: '$thingName${ if (shadowName == null) "" else "~$shadowName"}'"

        return ResourceNotFoundException.builder()
            .awsErrorDetails(
                AwsErrorDetails.builder()
                    .errorCode("ResourceNotFoundException")
                    .errorMessage(message)
                    .build()
            )
            .message(message)
            .build()
    }
}