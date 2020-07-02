package io.andrewohara.awsmock.sqs

import java.time.Duration

data class MockMessage(
        val id: String,
        val body: String,
        var delay: Duration
)