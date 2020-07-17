package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.secretsmanager.model.InvalidParameterException
import java.nio.ByteBuffer
import java.util.*

data class MockSecretValue(private var string: String?, private var binary: ByteBuffer?) {

    val version = UUID.randomUUID().toString()

    init {
        validate(string, binary)
    }

    fun update(string: String? = null, binary: ByteBuffer? = null) {
        validate(string, binary)
        this.string = string
        this.binary = binary
    }

    fun string() = string
    fun binary() = binary

    companion object {
        private fun validate(string: String?, binary: ByteBuffer?) {
            if (binary != null && string != null) {
                throw InvalidParameterException("You can't specify both a binary secret value and a string secret value in the same secret.").apply {
                    requestId = UUID.randomUUID().toString()
                    errorType = AmazonServiceException.ErrorType.Client
                    errorCode = "InvalidParameterException"
                    statusCode = 400
                }
            }
        }
    }
}