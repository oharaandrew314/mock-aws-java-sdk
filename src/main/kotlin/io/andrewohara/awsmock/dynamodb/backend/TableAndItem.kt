package io.andrewohara.awsmock.dynamodb.backend

data class TableAndItem(
    val tableName: TableName,
    val item: MockDynamoItem
)