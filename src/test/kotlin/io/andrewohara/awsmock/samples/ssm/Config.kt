package io.andrewohara.awsmock.samples.ssm

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest

data class Config(
        val catsHost: String,
        val catsApiKey: String
) {
    companion object {
        fun loadFromSsm(client: AWSSimpleSystemsManagement): Config {
            val parameters = GetParametersRequest().withNames("catsHost", "catsApiKey").withWithDecryption(true)
            val results = client.getParameters(parameters)
            
            return Config(
                    catsHost = results.parameters.first { it.name == "catsHost" }.value,
                    catsApiKey = results.parameters.first { it.name == "catsApiKey" }.value
            )
        }
    }
}