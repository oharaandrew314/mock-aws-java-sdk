package io.andrewohara.awsmock.samples.ssm

import io.andrewohara.awsmock.ssm.MockSsmV1
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ConfigTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV1(backend)

    @Test
    fun `load config`() {
        // setup parameters
        backend["catsHost"] = "https://cats.meow"
        backend.secure("catsApiKey", "hunter2")

        // perform test
        val config = Config.loadFromSsm(client)

        // verify
        config shouldBe Config(
            catsHost = "https://cats.meow",
            catsApiKey = "hunter2"
        )
    }
}