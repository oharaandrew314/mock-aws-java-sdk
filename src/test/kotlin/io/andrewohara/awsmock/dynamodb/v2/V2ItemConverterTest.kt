package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2.Companion.toMock
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2.Companion.toV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

class V2ItemConverterTest {

    @Test
    fun `mock null attribute to v2`() {
        MockDynamoValue().toV2() shouldBe AttributeValue.builder().nul(true).build()
    }
    @Test
    fun `v2 null attribute to mock`() {
        AttributeValue.builder().nul(true).build().toMock() shouldBe MockDynamoValue()
    }
}