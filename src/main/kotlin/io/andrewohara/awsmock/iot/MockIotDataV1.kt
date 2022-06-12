package io.andrewohara.awsmock.iot

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.iotdata.AWSIotData
import com.amazonaws.services.iotdata.model.*
import java.net.URI

class MockIotDataV1(private val backend: MockIotDataBackend = MockIotDataBackend()): AWSIotData {

    private var endpoint = URI.create("ssl://mock-iot-data")
    private var region: Region = Region.getRegion(Regions.CA_CENTRAL_1)

    override fun setEndpoint(endpoint: String) {
        this.endpoint = URI.create(endpoint)
    }

    override fun setRegion(region: Region) {
        this.region = region
    }

    override fun deleteThingShadow(request: DeleteThingShadowRequest): DeleteThingShadowResult {
        val payload = backend.delete(request.thingName, null)
            ?: throw ResourceNotFoundException("No shadow exists with name: '${request.thingName}'")

        return DeleteThingShadowResult()
            .withPayload(payload)
    }

    override fun getThingShadow(request: GetThingShadowRequest): GetThingShadowResult {
        val payload = backend[request.thingName, null]
            ?: throw ResourceNotFoundException("No shadow exists with name: '${request.thingName}'")

        return GetThingShadowResult()
            .withPayload(payload)
    }

    override fun updateThingShadow(request: UpdateThingShadowRequest): UpdateThingShadowResult {
        backend[request.thingName, null] = request.payload

        return UpdateThingShadowResult()
            .withPayload(request.payload)
    }

    override fun shutdown() {}
    override fun publish(publishRequest: PublishRequest) = throw UnsupportedOperationException()
    override fun getCachedResponseMetadata(request: AmazonWebServiceRequest) = throw UnsupportedOperationException()
}