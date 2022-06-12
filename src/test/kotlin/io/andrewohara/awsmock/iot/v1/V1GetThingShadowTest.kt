package io.andrewohara.awsmock.iot.v1

import com.amazonaws.services.iotdata.model.GetThingShadowRequest
import com.amazonaws.services.iotdata.model.GetThingShadowResult
import com.amazonaws.services.iotdata.model.ResourceNotFoundException
import io.andrewohara.awsmock.iot.IotDataTestFixtures
import io.andrewohara.awsmock.iot.MockIotDataBackend
import io.andrewohara.awsmock.iot.MockIotDataV1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


class V1GetThingShadowTest {

    private val backend = MockIotDataBackend()
    private val client = MockIotDataV1(backend)

    @Test
    fun `get shadow for missing thing`() {
        shouldThrow<ResourceNotFoundException> {
            client.getThingShadow(
                GetThingShadowRequest().withThingName("cat")
            )
        }.errorMessage shouldBe "No shadow exists with name: 'cat'"
    }

    @Test
    fun `get shadow for thing`() {
        backend["cat", null] = IotDataTestFixtures.payload1

        client.getThingShadow(
            GetThingShadowRequest().withThingName("cat")
        ) shouldBe GetThingShadowResult()
            .withPayload(IotDataTestFixtures.payload1)
    }

    @Test
    fun `get shadow for thing with named shadows`() {
        backend["cat", null] = IotDataTestFixtures.payload2
        backend["cat", "foot1"] = IotDataTestFixtures.payload1
        backend["cat", "paw2"] = IotDataTestFixtures.payload3

        client.getThingShadow(
            GetThingShadowRequest().withThingName("cat")
        ) shouldBe GetThingShadowResult()
            .withPayload(IotDataTestFixtures.payload2)
    }
}