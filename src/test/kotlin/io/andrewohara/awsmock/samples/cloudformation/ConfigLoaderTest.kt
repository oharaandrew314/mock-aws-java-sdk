package io.andrewohara.awsmock.samples.cloudformation

import io.andrewohara.awsmock.cloudformation.MockCloudformationBackend
import io.andrewohara.awsmock.cloudformation.MockCloudformationV2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConfigLoaderTest {

    private val backend = MockCloudformationBackend()
    private val testObj = ConfigLoader(
        cfn = MockCloudformationV2(backend)
    )

    @Test
    fun `load config`() {
        backend.createUpdate(
            "users",
            mapOf(
                "users-events-topic-arn" to "arn:aws:sns:01234567890:ca-central-1:topic:user-events-GHIJK",
                "users-table-name" to "users-ABCDEF"
            )
        )

        assertThat(testObj()).isEqualTo(Config(
            eventsTopicArn = "arn:aws:sns:01234567890:ca-central-1:topic:user-events-GHIJK",
            usersTableName = "users-ABCDEF"
        ))
    }
}