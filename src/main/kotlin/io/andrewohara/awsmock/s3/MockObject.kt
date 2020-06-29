package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.model.ObjectMetadata

class MockObject(
        val key: String,
        val content: ByteArray,
        val metadata: ObjectMetadata
)