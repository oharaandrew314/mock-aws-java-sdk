package io.andrewohara.awsmock.cloudformation.v1

import com.amazonaws.services.cloudformation.model.Export
import com.amazonaws.services.cloudformation.model.ListExportsRequest
import com.amazonaws.services.cloudformation.model.ListExportsResult
import io.andrewohara.awsmock.cloudformation.MockCloudformationBackend
import io.andrewohara.awsmock.cloudformation.MockCloudformationV1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MockCloudformationV1ListExportsTest {

    private val backend = MockCloudformationBackend()
    private val testObj = MockCloudformationV1(backend)

    @Test
    fun `list exports with no stacks`() {
        assertThat(testObj.listExports(ListExportsRequest())).isEqualTo(
            ListExportsResult()
        )
    }

    @Test
    fun `list exports for single stack`() {
        val stack = backend.createUpdate("stuff", mapOf("foo" to "bar", "toll" to "troll"))

        assertThat(testObj.listExports(ListExportsRequest())).isEqualTo(
            ListExportsResult().withExports(
                Export().withExportingStackId(stack.id).withName("foo").withValue("bar"),
                Export().withExportingStackId(stack.id).withName("toll").withValue("troll")
            )
        )
    }

    @Test
    fun `list exports for multiple stacks`() {
        val stack1 = backend.createUpdate("stuff", mapOf("foo" to "bar", "toll" to "troll"))
        val stack2 = backend.createUpdate("things", mapOf("animal" to "cat"))

        assertThat(testObj.listExports(ListExportsRequest())).isEqualTo(
            ListExportsResult().withExports(
                Export().withExportingStackId(stack1.id).withName("foo").withValue("bar"),
                Export().withExportingStackId(stack1.id).withName("toll").withValue("troll"),
                Export().withExportingStackId(stack2.id).withName("animal").withValue("cat"),
            )
        )
    }
}