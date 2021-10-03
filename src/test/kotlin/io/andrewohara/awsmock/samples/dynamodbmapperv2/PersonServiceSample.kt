package io.andrewohara.awsmock.samples.dynamodbmapperv2

import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import java.util.*

@DynamoDbBean
data class DynamoPerson(
    @get:DynamoDbPartitionKey var id: String = UUID.randomUUID().toString(),

    var name: String = "Unknown"
)


class PersonService(private val table: DynamoDbTable<DynamoPerson>) {

    fun register(name: String): UUID {
        val item = DynamoPerson(name = name)
        table.putItem(item)

        return UUID.fromString(item.id)
    }

    operator fun get(id: UUID): String? {
        val key = Key.builder().partitionValue(id.toString()).build()
        return table.getItem(key)?.name
    }
}

class PersonServiceTest {

    private val table = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(MockDynamoDbV2())
        .build()
        .table("people", TableSchema.fromBean(DynamoPerson::class.java))
        .also { it.createTable() }

    private val testObj = PersonService(table)

    @Test
    fun `register person`() {
        val id = testObj.register("Andrew")

        table.getItem(Key.builder().partitionValue(id.toString()).build()) shouldBe DynamoPerson(id = id.toString(), name = "Andrew")
    }

    @Test
    fun `get person`() {
        val id = UUID.randomUUID()
        val person = DynamoPerson(id = id.toString(), name = "Andrew")
        table.putItem(person)

        testObj[id] shouldBe "Andrew"
    }

    @Test
    fun `get missing person`() {
        testObj[UUID.randomUUID()].shouldBeNull()
    }
}