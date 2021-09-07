package io.andrewohara.awsmock.core

import java.lang.Exception

class MockAwsException(
    val statusCode: Int,
    val errorCode: String,
    message: String,
    val errorType: ErrorType = ErrorType.Client
): Exception(message) {
    enum class ErrorType { Client, Service, Unknown }
}