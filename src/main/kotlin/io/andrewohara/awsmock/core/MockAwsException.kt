package io.andrewohara.awsmock.core

import java.lang.Exception

class MockAwsException(
    val statusCode: Int,
    val errorCode: String,
    message: String
): Exception(message)