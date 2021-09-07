package io.andrewohara.awsmock.secretsmanager.backend

import io.andrewohara.awsmock.core.MockAwsException
import java.nio.ByteBuffer
import java.util.*

class MockSecretValue private constructor(val string: String?, val binary: ByteBuffer?) {

    companion object {
        fun create(string: String?, binary: ByteBuffer?) = when {
            string != null && binary != null -> throw MockAwsException(
                message = "You can't specify both a binary secret value and a string secret value in the same secret.",
                errorCode = "InvalidParameterException",
                statusCode = 400
            )
            string == null && binary == null -> null
            else -> MockSecretValue(string, binary)
        }
    }

    val versionId = UUID.randomUUID().toString()

    var stages = listOf("AWSCURRENT")
        private set

    fun makePrevious() {
        stages = listOf("AWSPREVIOUS")
    }
    fun makeObsolete() {
        stages = listOf()
    }
}