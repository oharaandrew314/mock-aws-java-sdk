package io.andrewohara.awsmock.s3.v1

import com.amazonaws.services.s3.model.AmazonS3Exception
import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import io.andrewohara.awsmock.s3.v1.S3Assertions.assertBucketNotEmpty
import io.andrewohara.awsmock.s3.v1.S3Assertions.assertIsBucketNotFound
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockS3V1DeleteBucketUnitTest {

    private val backend = MockS3Backend()
    private val testObj = MockS3V1(backend)

    @Test
    fun `delete bucket`() {
        val bucket = backend.create("bucket")!!

        testObj.deleteBucket(bucket.name)
        assertThat(backend.buckets()).isEmpty()
    }

    @Test
    fun `delete missing bucket`() {
        val exception = catchThrowableOfType({ testObj.deleteBucket("bucket") }, AmazonS3Exception::class.java)
        exception.assertIsBucketNotFound()
    }

    @Test
    fun `can't delete bucket with objects inside`() {
        val bucket = backend.create("bucket")!!
        bucket["foo"] = "bar"

        val exception = catchThrowableOfType({ testObj.deleteBucket(bucket.name) }, AmazonS3Exception::class.java)
        exception.assertBucketNotEmpty()
    }
}