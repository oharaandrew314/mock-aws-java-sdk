package io.andrewohara.awsmock.iot.v1

import com.amazonaws.services.iotdata.model.DeleteThingShadowRequest
import com.amazonaws.services.iotdata.model.ResourceNotFoundException
import io.andrewohara.awsmock.iot.IotDataTestFixtures
import io.andrewohara.awsmock.iot.MockIotDataBackend
import io.andrewohara.awsmock.iot.MockIotDataV1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class V1DeleteThingShadowTest {

    private val backend = MockIotDataBackend()
    private val client = MockIotDataV1(backend)

    @Test
    fun `delete missing thing shadow`() {
        shouldThrow<ResourceNotFoundException> {
            client.deleteThingShadow(
                DeleteThingShadowRequest().withThingName("cat")
            )
        }.errorMessage shouldBe "No shadow exists with name: 'cat'"
    }

    @Test
    fun `delete thing shadow`() {
        backend["cat", null] = IotDataTestFixtures.payload3

        client.deleteThingShadow(
            DeleteThingShadowRequest().withThingName("cat")
        ).payload shouldBe IotDataTestFixtures.payload3

        backend["cat", null] shouldBe null
    }
}