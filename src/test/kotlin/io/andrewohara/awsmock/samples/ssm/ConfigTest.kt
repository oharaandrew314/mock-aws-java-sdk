package io.andrewohara.awsmock.samples.ssm

import com.amazonaws.services.simplesystemsmanagement.model.ParameterType
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest
import io.andrewohara.awsmock.ssm.MockAWSSimpleSystemsManagement
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ConfigTest {

    private val client = MockAWSSimpleSystemsManagement()

    @Test
    fun `load config`() {
        // setup parameters
        client.putParameter(
                PutParameterRequest()
                        .withName("catsHost")
                        .withType(ParameterType.String)
                        .withValue("https://cats.meow")
        )
        client.putParameter(
                PutParameterRequest()
                        .withName("catsApiKey")
                        .withType(ParameterType.SecureString)
                        .withValue("hunter2")
        )

        // perform test
        val config = Config.loadFromSsm(client)

        // verify state
        Assertions.assertThat(config).isEqualTo(Config(
                catsHost = "https://cats.meow",
                catsApiKey = "hunter2"
        ))
    }
}