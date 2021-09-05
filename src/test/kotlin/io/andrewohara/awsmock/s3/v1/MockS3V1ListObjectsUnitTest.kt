package io.andrewohara.awsmock.s3.v1

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ListObjectsRequest
import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import io.andrewohara.awsmock.s3.v1.S3Assertions.assertIsBucketNotFound
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockS3V1ListObjectsUnitTest {

    private val backend = MockS3Backend()
    private val testObj = MockS3V1(backend)
    private val bucket = backend.create("bucket")!!

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
        bucket["obj1"] = "foo"
        bucket["obj2"] = "bar"

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
        bucket["foo/obj1"] = "bar"
        bucket["foo/obj2"] = "baz"
        bucket["toll/obj"] = "troll"

        val result = testObj.listObjects(bucket.name, "foo")
        assertThat(result.objectSummaries.map { it.key }).containsExactlyInAnyOrder("foo/obj1", "foo/obj2")
    }

    @Test
    fun `list objects with key limit`() {
        bucket["foo"] = "bar"
        bucket["toll"] = "troll"

        val result = testObj.listObjects(ListObjectsRequest().withBucketName(bucket.name).withMaxKeys(1))

        assertThat(result.objectSummaries).hasSize(1)
    }
}