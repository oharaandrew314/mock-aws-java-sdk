package io.andrewohara.awsmock.core

import com.amazonaws.AmazonServiceException

fun MockAwsException.ErrorType.toV1() = when (this) {
    MockAwsException.ErrorType.Client -> AmazonServiceException.ErrorType.Client
    MockAwsException.ErrorType.Service -> AmazonServiceException.ErrorType.Service
    MockAwsException.ErrorType.Unknown -> AmazonServiceException.ErrorType.Unknown
}