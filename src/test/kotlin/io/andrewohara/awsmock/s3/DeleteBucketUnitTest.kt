package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import io.andrewohara.awsmock.s3.S3Assertions.assertBucketNotEmpty
import io.andrewohara.awsmock.s3.S3Assertions.assertIsBucketNotFound
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class DeleteBucketUnitTest {

    private lateinit var testObj: AmazonS3

    @Before
    fun setup() {
        testObj = MockAmazonS3()
    }

    @Test
    fun `delete bucket`() {
        val bucket = testObj.createBucket("bucket")

        testObj.deleteBucket(bucket.name)
        assertThat(testObj.listBuckets()).isEmpty()
    }

    @Test
    fun `delete missing bucket`() {
        val exception = catchThrowableOfType({ testObj.deleteBucket("bucket") }, AmazonS3Exception::class.java)
        exception.assertIsBucketNotFound()
    }

    @Test
    fun `can't delete bucket with objects inside`() {
        val bucket = testObj.createBucket("bucket")
        testObj.putObject(bucket.name, "foo", "bar")

        val exception = catchThrowableOfType({ testObj.deleteBucket(bucket.name) }, AmazonS3Exception::class.java)
        exception.assertBucketNotEmpty()
    }
}