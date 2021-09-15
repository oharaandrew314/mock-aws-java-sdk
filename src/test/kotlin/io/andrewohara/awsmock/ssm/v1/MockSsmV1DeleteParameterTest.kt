package io.andrewohara.awsmock.ssm.v1

import com.amazonaws.services.simplesystemsmanagement.model.DeleteParameterRequest
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException
import io.andrewohara.awsmock.ssm.MockSsmV1
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import org.junit.jupiter.api.Test

class MockSsmV1DeleteParameterTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV1(backend)

    @Test
    fun `delete missing`() {
        val request = DeleteParameterRequest().withName("foo")

        shouldThrow<ParameterNotFoundException> {
            client.deleteParameter(request)
        }
    }

    @Test
    fun `delete parameter`() {
        backend["foo"] = "bar"

        val request = DeleteParameterRequest().withName("foo")

        client.deleteParameter(request)

        backend["foo"].shouldBeNull()
    }
}