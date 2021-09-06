package io.andrewohara.awsmock.sqs.backend

data class MockSqsReceipt internal constructor(
    val receiptHandle: String,
    val message: MockSqsMessage
)