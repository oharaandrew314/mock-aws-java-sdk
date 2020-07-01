package io.andrewohara.awsmock.s3

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.assertj.core.api.Assertions

object S3Assertions {

    fun AmazonS3Exception.assertIsBucketNotFound() {
        Assertions.assertThat(errorMessage).isEqualTo("The specified bucket does not exist")
        Assertions.assertThat(errorCode).isEqualTo("NoSuchBucket")
        Assertions.assertThat(statusCode).isEqualTo(404)
        Assertions.assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }

    fun AmazonS3Exception.assertIsNotFound() {
        Assertions.assertThat(errorMessage).isEqualTo("Not Found")
        Assertions.assertThat(errorCode).isEqualTo("404 Not Found")
        Assertions.assertThat(statusCode).isEqualTo(404)
        Assertions.assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }

    fun AmazonS3Exception.assertBucketNotEmpty() {
        Assertions.assertThat(errorMessage).isEqualTo("The bucket you tried to delete is not empty")
        Assertions.assertThat(errorCode).isEqualTo("BucketNotEmpty")
        Assertions.assertThat(statusCode).isEqualTo(409)
        Assertions.assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }

    fun AmazonS3Exception.assertIsNoSuchKey() {
        Assertions.assertThat(errorMessage).isEqualTo("The specified key does not exist")
        Assertions.assertThat(errorCode).isEqualTo("NoSuchKey")
        Assertions.assertThat(statusCode).isEqualTo(404)
        Assertions.assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }
}