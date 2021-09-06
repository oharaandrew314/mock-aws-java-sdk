package io.andrewohara.awsmock.sqs.backend

import java.time.Instant

data class MockSqsMessage internal constructor(
    val id: String,
    val body: String,
    var visibleAt: Instant
)