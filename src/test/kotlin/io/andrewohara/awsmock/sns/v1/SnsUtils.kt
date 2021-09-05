package io.andrewohara.awsmock.sns.v1

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.sns.model.AmazonSNSException
import com.amazonaws.services.sns.model.InvalidParameterException
import com.amazonaws.services.sns.model.NotFoundException
import org.assertj.core.api.Assertions.*

object SnsUtils {

    fun AmazonSNSException.assertMissingParameter(parameter: String) {
        assertThat(errorMessage).isEqualTo("1 validation error detected: Value null at '$parameter' failed to satisfy constraint: Member must not be null")
        assertThat(errorCode).isEqualTo("ValidationError")
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }

    fun InvalidParameterException.assertInvalidParameter(parameter: String) {
        assertThat(errorMessage).isEqualTo("Invalid parameter: $parameter Reason: no value for required parameter")
        assertThat(errorCode).isEqualTo("InvalidParameter")
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }

    fun NotFoundException.assertNotFound() {
        assertThat(errorMessage).isEqualTo("Topic does not exist")
        assertThat(errorCode).isEqualTo("NotFound")
        assertThat(statusCode).isEqualTo(404)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }
}