package io.andrewohara.awsmock.sqs.backend

import java.time.Instant

data class MockSqsMessage(
    val id: String,
    val body: String,
    var visibleAt: Instant
)