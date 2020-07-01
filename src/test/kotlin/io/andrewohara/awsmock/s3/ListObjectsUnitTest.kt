package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.Bucket
import io.andrewohara.awsmock.s3.S3Assertions.assertIsBucketNotFound
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class ListObjectsUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

    @Test
    fun `list objects for bucket that doesn't exist`() {
        val exception = catchThrowableOfType({ testObj.listObjects("missingBucket") }, AmazonS3Exception::class.java)
        exception.assertIsBucketNotFound()
    }

    @Test
    fun `list objects for empty bucket`() {
        val result = testObj.listObjects(bucket.name)

        assertThat(result.bucketName).isEqualTo(bucket.name)
        assertThat(result.prefix).isEqualTo(null)
        assertThat(result.objectSummaries).isEmpty()
    }

    @Test
    fun `list objects for bucket`() {
        testObj.putObject(bucket.name, "obj1", "foo")
        testObj.putObject(bucket.name, "obj2", "bar")

        val result = testObj.listObjects(bucket.name)
        assertThat(result.bucketName).isEqualTo(bucket.name)
        assertThat(result.objectSummaries).hasSize(2)

        result.objectSummaries[0].let { obj1 ->
            assertThat(obj1.bucketName).isEqualTo(bucket.name)
            assertThat(obj1.key).isEqualTo("obj1")
        }

        result.objectSummaries[1].let { obj1 ->
            assertThat(obj1.bucketName).isEqualTo(bucket.name)
            assertThat(obj1.key).isEqualTo("obj2")
        }
    }

    @Test
    fun `list objects with with prefix`() {
        testObj.putObject(bucket.name, "foo/obj", "bar")
        testObj.putObject(bucket.name, "toll/obj", "troll")

        val result = testObj.listObjects(bucket.name, "foo")
        assertThat(result.bucketName).isEqualTo(bucket.name)
        assertThat(result.prefix).isEqualTo("foo")
        assertThat(result.objectSummaries).hasSize(1)

        result.objectSummaries[0].let { obj1 ->
            assertThat(obj1.bucketName).isEqualTo(bucket.name)
            assertThat(obj1.key).isEqualTo("foo/obj")
        }
    }
}