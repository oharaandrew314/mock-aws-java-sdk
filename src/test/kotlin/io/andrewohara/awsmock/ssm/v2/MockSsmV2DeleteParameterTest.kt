package io.andrewohara.awsmock.ssm.v2

import io.andrewohara.awsmock.ssm.MockSsmV2
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException

class MockSsmV2DeleteParameterTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV2(backend)

    @Test
    fun `delete missing`() {
        shouldThrow<ParameterNotFoundException> {
            client.deleteParameter {
                it.name("foo")
            }
        }
    }

    @Test
    fun `delete parameter`() {
        backend["foo"] = "bar"

        client.deleteParameter {
            it.name("foo")
        }

        backend["foo"].shouldBeNull()
    }
}