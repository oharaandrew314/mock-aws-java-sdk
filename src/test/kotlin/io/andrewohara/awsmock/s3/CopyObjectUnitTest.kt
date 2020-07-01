package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.Bucket
import io.andrewohara.awsmock.s3.S3Assertions.assertIsBucketNotFound
import io.andrewohara.awsmock.s3.S3Assertions.assertIsNoSuchKey
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class CopyObjectUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

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
        testObj.putObject(bucket.name, "foo", "bar")

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
        testObj.putObject(bucket.name, "foo", "bar")

        testObj.copyObject(bucket.name, "foo", bucket.name, "toll")

        assertThat(testObj.listObjectsV2(bucket.name).keyCount).isEqualTo(2)
        assertThat(testObj.getObjectAsString(bucket.name, "foo")).isEqualTo("bar")
        assertThat(testObj.getObjectAsString(bucket.name, "toll")).isEqualTo("bar")
    }

    @Test
    fun `copy object to existing bucket and existing key`() {
        testObj.putObject(bucket.name, "foo", "bar")
        testObj.putObject(bucket.name, "toll", "troll")

        testObj.copyObject(bucket.name, "foo", bucket.name, "toll")

        assertThat(testObj.listObjectsV2(bucket.name).keyCount).isEqualTo(2)
        assertThat(testObj.getObjectAsString(bucket.name, "foo")).isEqualTo("bar")
        assertThat(testObj.getObjectAsString(bucket.name, "toll")).isEqualTo("bar")
    }

    @Test
    fun `copy object to new bucket and new key`() {
        val bucket2 = testObj.createBucket("bucket2")
        testObj.putObject(bucket.name, "foo", "bar")

        testObj.copyObject(bucket.name, "foo", bucket2.name, "foo")

        assertThat(testObj.listObjectsV2(bucket.name).keyCount).isEqualTo(1)
        assertThat(testObj.listObjectsV2(bucket2.name).keyCount).isEqualTo(1)
        assertThat(testObj.getObjectAsString(bucket.name, "foo")).isEqualTo("bar")
        assertThat(testObj.getObjectAsString(bucket2.name, "foo")).isEqualTo("bar")
    }

    @Test
    fun `copy object to new bucket and existing key`() {
        val bucket2 = testObj.createBucket("bucket2")
        testObj.putObject(bucket.name, "foo", "bar")
        testObj.putObject(bucket2.name, "foo", "baz")

        testObj.copyObject(bucket.name, "foo", bucket2.name, "foo")

        assertThat(testObj.listObjectsV2(bucket.name).keyCount).isEqualTo(1)
        assertThat(testObj.listObjectsV2(bucket2.name).keyCount).isEqualTo(1)
        assertThat(testObj.getObjectAsString(bucket.name, "foo")).isEqualTo("bar")
        assertThat(testObj.getObjectAsString(bucket2.name, "foo")).isEqualTo("bar")
    }
}