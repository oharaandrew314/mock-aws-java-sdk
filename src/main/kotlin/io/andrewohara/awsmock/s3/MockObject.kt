package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.model.ObjectMetadata

class MockObject(
        val content: ByteArray,
        val metadata: ObjectMetadata
) {
    fun copy() = MockObject(
            content = content.copyOf(),
            metadata = ObjectMetadata().clone()
    )
}