package io.andrewohara.awsmock.s3.v1

import com.amazonaws.services.s3.model.AmazonS3Exception
import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockS3V1DeleteObjectUnitTest {

    private val backend = MockS3Backend()
    private val bucket = backend.create("bucket")!!
    private val testObj = MockS3V1(backend)

    @Test
    fun `delete object from missing bucket`() {
        val exception = catchThrowableOfType( { testObj.deleteObject("missingBucket", "foo") }, AmazonS3Exception::class.java)

        assertThat(exception.errorMessage).isEqualTo("The specified bucket does not exist")
        assertThat(exception.errorCode).isEqualTo("NoSuchBucket")
        assertThat(exception.statusCode).isEqualTo(404)
    }

    @Test
    fun `delete missing object`() {
        // nothing should happen
        testObj.deleteObject(bucket.name, "foo")
    }

    @Test
    fun `delete object`() {
        bucket["foo"] = "bar"

        testObj.deleteObject(bucket.name, "foo")
        assertThat(bucket["foo"]).isNull()
    }
}