package io.andrewohara.awsmock.iot.v1

import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest
import io.andrewohara.awsmock.iot.IotDataTestFixtures
import io.andrewohara.awsmock.iot.MockIotDataBackend
import io.andrewohara.awsmock.iot.MockIotDataV1
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class V1UpdateThingShadowTest {

    private val backend = MockIotDataBackend()
    private val client = MockIotDataV1(backend)

    @Test
    fun `update non-existent shadow`() {
        client.updateThingShadow(
            UpdateThingShadowRequest()
                .withThingName("cat")
                .withPayload(IotDataTestFixtures.payload1)
        ).payload shouldBe IotDataTestFixtures.payload1

        backend["cat", null] shouldBe IotDataTestFixtures.payload1
    }

    @Test
    fun `update thing with existing shadow`() {
        backend["cat", null] = IotDataTestFixtures.payload1

        client.updateThingShadow(
            UpdateThingShadowRequest()
                .withThingName("cat")
                .withPayload(IotDataTestFixtures.payload2)
        ).payload shouldBe IotDataTestFixtures.payload2

        backend["cat", null] shouldBe IotDataTestFixtures.payload2
    }
}