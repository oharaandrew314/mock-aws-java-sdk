package io.andrewohara.awsmock.dynamodb.backend

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
        private val haB = MockDynamoValue(b = "ha".toB())
        private val stuffB = MockDynamoValue(b = "stuff".toB())
    }

    @Test
    fun `s contains subset of s`() {
        Conditions.contains(ha)(hai) shouldBe true
    }

    @Test
    fun `s contains s`() {
        Conditions.contains(hai)(hai) shouldBe true
    }

    @Test
    fun `s does not contain unrelated s`() {
        Conditions.contains(stuff)(hai) shouldBe false
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
        Conditions.contains(stuffB)(haiB) shouldBe false
    }
}