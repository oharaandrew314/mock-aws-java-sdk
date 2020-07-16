package io.andrewohara.awsmock.ssm

import com.amazonaws.services.simplesystemsmanagement.model.DeleteParameterRequest
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException
import io.andrewohara.awsmock.ssm.SsmUtils.assertIsCorrect
import io.andrewohara.awsmock.ssm.SsmUtils.get
import io.andrewohara.awsmock.ssm.SsmUtils.set
import org.assertj.core.api.Assertions.*
import org.junit.Test

class DeleteParameterTest {

    private val client = MockAWSSimpleSystemsManagement()

    @Test
    fun `delete missing`() {
        val request = DeleteParameterRequest().withName("foo")

        val exception = catchThrowableOfType(
                { client.deleteParameter(request) },
                ParameterNotFoundException::class.java
        )

        exception.assertIsCorrect()
    }

    @Test
    fun `delete parameter`() {
        client["foo"] = "bar"

        val request = DeleteParameterRequest().withName("foo")

        client.deleteParameter(request)

        assertThat(client["foo"]).isNull()
    }
}