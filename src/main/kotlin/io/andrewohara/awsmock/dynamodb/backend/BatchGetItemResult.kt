package io.andrewohara.awsmock.dynamodb.backend

import io.andrewohara.awsmock.core.MockAwsException

data class BatchGetItemResult(
    val results: List<TableAndItem>,
    val unprocessed: List<TableAndItem>,
) {
    fun throwIfUnprocessed(): BatchGetItemResult {
        if (unprocessed.isNotEmpty()) {
            throw MockAwsException(400, "ValidationError", "key limit exceeded")
        }
        return this
    }
}