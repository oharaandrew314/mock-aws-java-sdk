package io.andrewohara.awsmock.s3.v1

import com.amazonaws.services.s3.model.AmazonS3Exception
import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import io.andrewohara.awsmock.s3.v1.S3Assertions.assertIsBucketNotFound
import io.andrewohara.awsmock.s3.v1.S3Assertions.assertIsNoSuchKey
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockS3V1CopyObjectUnitTest {

    private val backend = MockS3Backend()
    private val bucket = backend.create("bucket")!!
    private val testObj = MockS3V1(backend)

    @Test
    fun `copy object from bucket that doesn't exist`() {
        val exception = catchThrowableOfType(
                { testObj.copyObject("missingBucket", "foo", bucket.name, "foo") },
                AmazonS3Exception::class.java
        )
        exception.assertIsBucketNotFound()
    }

    @Test
    fun `copy object to bucket that doesn't exist`() {
        bucket["foo"] = "bar"

        val exception = catchThrowableOfType(
                { testObj.copyObject(bucket.name, "foo", "missingBucket", "foo") },
                AmazonS3Exception::class.java
        )
        exception.assertIsBucketNotFound()
    }

    @Test
    fun `copy object that doesn't exist`() {
        val exception = catchThrowableOfType(
                { testObj.copyObject(bucket.name, "foo", bucket.name, "bar") },
                AmazonS3Exception::class.java
        )
        exception.assertIsNoSuchKey()
    }

    @Test
    fun `copy object to existing bucket and new key`() {
        bucket["foo"] = "bar"

        testObj.copyObject(bucket.name, "foo", bucket.name, "toll")

        assertThat(bucket.keys()).hasSize(2)
        assertThat(bucket.getString("foo")).isEqualTo("bar")
        assertThat(bucket.getString("toll")).isEqualTo("bar")
    }

    @Test
    fun `copy object to existing bucket and existing key`() {
        bucket["foo"] = "bar"
        bucket["toll"] = "troll"

        testObj.copyObject(bucket.name, "foo", bucket.name, "toll")

        assertThat(bucket.keys()).hasSize(2)
        assertThat(bucket.getString("foo")).isEqualTo("bar")
        assertThat(bucket.getString("toll")).isEqualTo("bar")
    }

    @Test
    fun `copy object to new bucket`() {
        val bucket2 = backend.create("bucket2")!!
        bucket["foo"] = "bar"

        testObj.copyObject(bucket.name, "foo", bucket2.name, "foo")

        assertThat(bucket.keys()).hasSize(1)
        assertThat(bucket2.keys()).hasSize(1)
        assertThat(bucket.getString("foo")).isEqualTo("bar")
        assertThat(bucket2.getString("foo")).isEqualTo("bar")
    }

    @Test
    fun `copy object to new bucket, replacing existing object`() {
        val bucket2 = backend.create("bucket2")!!
        bucket["foo"] = "bar"
        bucket2["foo"] = "baz"

        testObj.copyObject(bucket.name, "foo", bucket2.name, "foo")

        assertThat(bucket.keys()).hasSize(1)
        assertThat(bucket.keys()).hasSize(1)
        assertThat(bucket.getString("foo")).isEqualTo("bar")
        assertThat(bucket2.getString("foo")).isEqualTo("bar")
    }
}