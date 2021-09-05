package io.andrewohara.awsmock.cloudformation.v2

import io.andrewohara.awsmock.cloudformation.MockCloudformationBackend
import io.andrewohara.awsmock.cloudformation.MockCloudformationV2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.cloudformation.model.Export
import software.amazon.awssdk.services.cloudformation.model.ListExportsResponse

class MockCloudformationV2ListExportsTest {

    private val backend = MockCloudformationBackend()
    private val testObj = MockCloudformationV2(backend)

    @Test
    fun `list exports with no stacks`() {
        assertThat(testObj.listExports()).isEqualTo(
            ListExportsResponse.builder()
                .exports(emptyList())
                .build()
        )
    }

    @Test
    fun `list exports for single stack`() {
        val stack = backend.createUpdate("stuff", mapOf("foo" to "bar", "toll" to "troll"))


        assertThat(testObj.listExports()).isEqualTo(
            ListExportsResponse.builder()
                .exports(
                    Export.builder().exportingStackId(stack.id).name("foo").value("bar").build(),
                    Export.builder().exportingStackId(stack.id).name("toll").value("troll").build()
                )
                .build()
        )
    }

    @Test
    fun `list exports for multiple stacks`() {
        val stack1 = backend.createUpdate("stuff", mapOf("foo" to "bar", "toll" to "troll"))
        val stack2 = backend.createUpdate("things", mapOf("animal" to "cat"))

        assertThat(testObj.listExports()).isEqualTo(
            ListExportsResponse.builder()
                .exports(
                    Export.builder().exportingStackId(stack1.id).name("foo").value("bar").build(),
                    Export.builder().exportingStackId(stack1.id).name("toll").value("troll").build(),
                    Export.builder().exportingStackId(stack2.id).name("animal").value("cat").build(),
                )
                .build()
        )
    }
}