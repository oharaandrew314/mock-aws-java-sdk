package io.andrewohara.awsmock.sqs.backend

data class MockSqsReceipt(
    val receiptHandle: String,
    val message: MockSqsMessage
)