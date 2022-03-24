package io.andrewohara.awsmock.samples.dynamodbmapperv2

import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.*
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.Projection
import software.amazon.awssdk.services.dynamodb.model.ProjectionType
import java.util.*

@DynamoDbBean
data class DynamoV2Person(
    @get:DynamoDbPartitionKey var id: String = UUID.randomUUID().toString(),

    @get:DynamoDbSecondaryPartitionKey(indexNames = ["names"])
    var name: String? = null,

    var gender: String? = null,
) {
    fun toModel() = Person(
        id = UUID.fromString(id),
        name = name ?: "unknown",
        gender = gender ?: "unknown"
    )

    companion object {
        fun create(dynamo: DynamoDbEnhancedClient): DynamoDbTable<DynamoV2Person> {
            val table = dynamo.table("person", BeanTableSchema.create(DynamoV2Person::class.java))

            table.createTable {
                it.globalSecondaryIndices(
                    EnhancedGlobalSecondaryIndex.builder().indexName("names").projection(
                        Projection.builder().projectionType(ProjectionType.ALL).build()
                    ).build()
                )
            }

            return table
        }
    }
}

data class Person(val id: UUID, val name: String, val gender: String)


class PersonService(private val table: DynamoDbTable<DynamoV2Person>) {

    fun register(name: String, gender: String): Person {
        val id = UUID.randomUUID()
        val item = DynamoV2Person(id = id.toString(), name = name, gender = gender)
        table.putItem(item)

        return Person(id = id, name = name, gender = gender)
    }

    operator fun get(id: UUID): Person? {
        val key = Key.builder().partitionValue(id.toString()).build()
        return table.getItem(key)?.toModel()
    }

    operator fun get(name: String, gender: String): Person? {
         return table.index("names")
            .query { builder: QueryEnhancedRequest.Builder ->
                builder.queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(name).build())
                )
                builder.filterExpression(Expression.builder()
                    .expression("gender = :gender")
                    .putExpressionValue(":gender", AttributeValue.builder().s(gender).build())
                    .build()
                )
            }
             .asSequence()
             .flatMap { it.items() }
             .map { it.toModel() }
             .firstOrNull()
    }
}

class PersonServiceTest {

    private val table = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(MockDynamoDbV2())
        .build()
        .let { DynamoV2Person.create(it) }

    private val testObj = PersonService(table)

    @Test
    fun `register person`() {
        val person = testObj.register("Andrew", "male")

        val key = Key.builder().partitionValue(person.id.toString()).build()
        table.getItem(key) shouldBe DynamoV2Person(
            id = person.id.toString(),
            name = "Andrew",
            gender = "male"
        )
    }

    @Test
    fun `get person`() {
        val id = UUID.randomUUID()
        val person = DynamoV2Person(id = id.toString(), name = "Andrew", gender = "male")
        table.putItem(person)

        testObj[id] shouldBe Person(
            id = id,
            name = "Andrew",
            gender = "male"
        )
    }

    @Test
    fun `get missing person`() {
        testObj[UUID.randomUUID()].shouldBeNull()
    }

    @Test
    fun `get by name and gender`() {
        val male = DynamoV2Person(name = "Alex", gender = "male")
        val female = DynamoV2Person(name = "Alex", gender = "female")

        table.putItem(male)
        table.putItem(female)

        testObj["Alex", "female"] shouldBe female.toModel()
    }

    @Test
    fun `get by name and incorrect gender`() {
        val male = DynamoV2Person(name = "Alex", gender = "male")
        table.putItem(male)

        testObj["Alex", "female"].shouldBeNull()
    }
}