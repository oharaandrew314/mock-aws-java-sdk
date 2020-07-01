package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.Bucket
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class DeleteObjectUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

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
        testObj.putObject(bucket.name, "foo", "bar")

        testObj.deleteObject(bucket.name, "foo")
        assertThat(testObj.doesObjectExist(bucket.name, "foo")).isFalse()
    }
}