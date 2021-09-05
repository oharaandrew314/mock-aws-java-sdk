package io.andrewohara.awsmock.s3.v1

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockS3V1DeleteObjectsUnitTest {

    private val backend = MockS3Backend()
    private val bucket = backend.create("bucket")!!
    private val testObj = MockS3V1(backend)

    @Test
    fun `delete objects from bucket that doesn't exist`() {
        val request = DeleteObjectsRequest("missingBucket").withKeys("toll", "spam")

        val exception = catchThrowableOfType( { testObj.deleteObjects(request) }, AmazonS3Exception::class.java)

        assertThat(exception.errorMessage).isEqualTo("The specified bucket does not exist")
        assertThat(exception.errorCode).isEqualTo("NoSuchBucket")
        assertThat(exception.statusCode).isEqualTo(404)
    }

    @Test
    fun `delete objects that don't exist`() {
        bucket["foo"] = "bar"

        val request = DeleteObjectsRequest(bucket.name).withKeys("toll", "spam")
        val result = testObj.deleteObjects(request)

        assertThat(result.deletedObjects).isEmpty()
    }

    @Test
    fun `delete objects`() {
        bucket["foo"] = "bar"
        bucket["toll"] = "troll"
        bucket["spam"] = "ham"

        val request = DeleteObjectsRequest(bucket.name).withKeys("foo", "toll")
        val result = testObj.deleteObjects(request)
        assertThat(result.deletedObjects.map { it.key }).containsExactlyInAnyOrder("foo", "toll")

        assertThat(bucket.keys()).containsExactly("spam")
    }
}