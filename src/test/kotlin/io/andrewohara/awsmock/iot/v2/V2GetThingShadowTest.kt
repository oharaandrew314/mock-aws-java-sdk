package io.andrewohara.awsmock.iot.v2

import io.andrewohara.awsmock.iot.IotDataTestFixtures
import io.andrewohara.awsmock.iot.MockIotDataBackend
import io.andrewohara.awsmock.iot.MockIotDataV2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException

class V2GetThingShadowTest {

    private val backend = MockIotDataBackend()
    private val client = MockIotDataV2(backend)

    @Test
    fun `get shadow for missing thing`() {
        shouldThrow<ResourceNotFoundException> {
            client.getThingShadow {
                it.thingName("cat")
            }
        }.awsErrorDetails().errorMessage() shouldBe "No shadow exists with name: 'cat'"
    }

    @Test
    fun `get named shadow for missing thing`() {
        shouldThrow<ResourceNotFoundException> {
            client.getThingShadow {
                it.thingName("cat")
                it.shadowName("snacks")
            }
        }.awsErrorDetails().errorMessage() shouldBe "No shadow exists with name: 'cat~snacks'"
    }

    @Test
    fun `get shadow for thing`() {
        backend["cat", null] = IotDataTestFixtures.payload3
        backend["cat", "snacks"] = IotDataTestFixtures.payload2

        client.getThingShadow {
            it.thingName("cat")
        }.payload() shouldBe SdkBytes.fromByteBuffer(IotDataTestFixtures.payload3)
    }

    @Test
    fun `get named shadow for thing`() {
        backend["cat", null] = IotDataTestFixtures.payload3
        backend["cat", "snacks"] = IotDataTestFixtures.payload2

        client.getThingShadow {
            it.thingName("cat")
            it.shadowName("snacks")
        }.payload() shouldBe SdkBytes.fromByteBuffer(IotDataTestFixtures.payload2)
    }
}