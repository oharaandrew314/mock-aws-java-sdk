package io.andrewohara.awsmock.dynamodb.backend

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class MockDynamoCondition2Test {

    companion object {
        private fun String.toB() = ByteBuffer.wrap(encodeToByteArray())

        private val hai = MockDynamoValue(s = "hai")
        private val ha = MockDynamoValue(s = "ha")
        private val stuff = MockDynamoValue(s = "stuff")

        private val haiB = MockDynamoValue(b = "hai".toB())
//        private val haB = MockDynamoValue(b = "ha".toB())
        private val stuffB = MockDynamoValue(b = "stuff".toB())

        private val itemHai = MockDynamoItem(
            "str" to hai,
            "bin" to haiB
        )

        private val itemStuff = MockDynamoItem(
            "str" to stuff,
            "bin" to stuffB
        )

        private val female = MockDynamoValue("female")
        private val male = MockDynamoValue("male")
        private val togglesName = MockDynamoValue("Toggles")
        private val smokeyName = MockDynamoValue("Smokey")
        private val banditName = MockDynamoValue("Bandit")

        private val toggles = MockDynamoItem("name" to togglesName, "gender" to female)
        private val smokey = MockDynamoItem("name" to smokeyName, "gender" to female)
        private val bandit = MockDynamoItem("name" to banditName, "gender" to male)


    }

    @Test
    fun `hai contains ha`() {
        "str".contains(ha)(itemHai) shouldBe true
    }

    @Test
    fun `hai contains hai`() {
        "str".contains(hai)(itemHai) shouldBe true
    }

    @Test
    fun `hai contains stuff`() {
        "str".contains(stuff)(itemHai) shouldBe false
    }

    @Test
    fun `hai equals hai`() {
        "str".eq(hai)(itemHai) shouldBe true
    }

    @Test
    fun `hai equals ha`() {
        "str".eq(ha)(itemHai) shouldBe false
    }

    @Test
    fun `ha does not equal hai`() {
        "str".eq(ha).not()(itemHai) shouldBe true
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
    fun `haiB contains stuffB`() {
        "bin".contains(stuffB)(itemHai) shouldBe false
    }

    @Test
    fun `hai equals hai and hai contains ha`() {
        val cond = "str".eq(hai) and "str".contains(ha)
        cond(itemHai) shouldBe true
        cond(itemStuff) shouldBe false
    }

    @Test
    fun `hai equals ha or hai equals hai`() {
        val cond = "str".eq(ha) or "str".eq(hai)
        cond(itemHai) shouldBe true
        cond(itemStuff) shouldBe false
    }

    @Test
    fun `parse gender eq female`() {
        val cond = MockDynamoCondition2.parseExpression("gender = :i1", ":i1" to female)
        cond(toggles) shouldBe true
        cond(smokey) shouldBe true
        cond(bandit) shouldBe false
    }

    @Test
    fun `parse gender eq female and name eq Toggles`() {
        val cond = MockDynamoCondition2.parseExpression("gender = :i1 and name = :i2", ":i1" to female, ":i2" to togglesName)
        cond(toggles) shouldBe true
        cond(smokey) shouldBe false
        cond(bandit) shouldBe false
    }

    @Test
    fun `parse (gender eq female and name eq toggles) or name = Bandit`() {
        val cond = MockDynamoCondition2.parseExpression(
            "(gender = :i1 and name = :i2) or name = :i3",
            ":i1" to female, ":i2" to togglesName, ":i3" to banditName
        )
        cond(toggles) shouldBe true
        cond(smokey) shouldBe false
        cond(bandit) shouldBe true
    }
}