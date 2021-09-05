package io.andrewohara.awsmock.cloudformation

import java.util.*

class MockCloudformationBackend {
    private val stacks = mutableMapOf<String, MockCfnStack>()
    fun stacks() = stacks.values

    fun createUpdate(name: String, exports: Map<String, String>): MockCfnStack {

        return MockCfnStack(
            id = UUID.randomUUID().toString(),
            exports = exports.toMap()
        ).also { stacks[name] = it }
    }
}

data class MockCfnStack(
    val id: String,
    val exports: Map<String, String>
)