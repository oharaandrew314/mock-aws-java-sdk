package io.andrewohara.awsmock.dynamodb.backend

import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.contains
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.eq
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class MockDynamoConditionTest {

    companion object {
        private fun String.toB() = ByteBuffer.wrap(encodeToByteArray())

        private val hai = MockDynamoValue(s = "hai")
        private val ha = MockDynamoValue(s = "ha")
        private val stuff = MockDynamoValue(s = "stuff")

        private val haiB = MockDynamoValue(b = "hai".toB())
//        private val haB = MockDynamoValue(b = "ha".toB())
        private val stuffB = MockDynamoValue(b = "stuff".toB())

        private val item = MockDynamoItem(
            "str" to hai,
            "bin" to haiB
        )
    }

    @Test
    fun `s contains subset of s`() {
        val cond = contains(ha)
        cond("str", item) shouldBe true
    }

    @Test
    fun `s contains s`() {
        val cond = contains(hai)
        cond("str", item) shouldBe true
    }

    @Test
    fun `s does not contain unrelated s`() {
        val cond = contains(stuff)
        cond("str", item) shouldBe false
    }

//    @Test TODO implement
//    fun `b contains subset of b`() {
//        Conditions.contains(haB)(haiB) shouldBe true
//    }

//    @Test TODO implement
//    fun `b contains b`() {
//        Conditions.contains(haiB)(haiB) shouldBe true
//    }
//
    @Test
    fun `b does not contain unrelated b`() {
        val cond = contains(stuffB)
        cond("bin", item) shouldBe false
    }

    @Test
    fun `parse name = Toggles`() {
        val conditions = MockDynamoCondition.parseExpression("name = :name", ":name" to MockDynamoValue(s = "Toggles"))
        conditions shouldBe mapOf("name" to eq(MockDynamoValue(s = "Toggles")))
    }

    @Test
    fun `parse ownerId = 2 and name = Toggles`() {
        val conditions = MockDynamoCondition.parseExpression(
            "ownerId = :ownerId and name = :name",
            ":name" to MockDynamoValue(s = "Toggles"), ":ownerId" to MockDynamoValue(n = 2)
        )
        conditions shouldBe mapOf(
            "ownerId" to eq(MockDynamoValue(n = 2)),
            "name" to eq(MockDynamoValue(s = "Toggles"))
        )
    }
}