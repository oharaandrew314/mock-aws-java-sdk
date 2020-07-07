package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.s3.model.GetObjectRequest
import io.andrewohara.awsmock.s3.S3Assertions.assertIsBucketNotFound
import io.andrewohara.awsmock.s3.S3Assertions.assertIsNoSuchKey
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test
import java.nio.file.Files

class GetObjectUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

    @Test
    fun `get object from missing bucket`() {
        val exception = catchThrowableOfType({ testObj.getObject("missingBucket", "foo") }, AmazonS3Exception::class.java)
        exception.assertIsBucketNotFound()
    }

    @Test
    fun `get missing object`() {
        val exception = catchThrowableOfType({ testObj.getObject(bucket.name, "foo") }, AmazonS3Exception::class.java)
        exception.assertIsNoSuchKey()
    }

    @Test
    fun `get missing object and save it to file`() {
        val dest = Files.createTempFile("foo", ".txt")
        dest.toFile().deleteOnExit()
        val request = GetObjectRequest(bucket.name, "foo")

        val exception = catchThrowableOfType({ testObj.getObject(request, dest.toFile()) }, AmazonS3Exception::class.java)
        exception.assertIsNoSuchKey()
    }

    @Test
    fun `get object and save it to file`() {
        testObj.putObject(bucket.name, "foo", "bar")

        val dest = Files.createTempFile("foo", ".txt")
        dest.toFile().deleteOnExit()
        val request = GetObjectRequest(bucket.name, "foo")

        val metadata = testObj.getObject(request, dest.toFile())
        assertThat(metadata.contentType).isEqualTo("text/plain")
        assertThat(dest.toFile().readText()).isEqualTo("bar")
    }

    // as string

    @Test
    fun `get object as string`() {
        testObj.putObject(bucket.name, "foo", "bar")

        assertThat(testObj.getObjectAsString(bucket.name, "foo")).isEqualTo("bar")
    }

    @Test
    fun `get missing object as string`() {
        val exception = catchThrowableOfType({ testObj.getObjectAsString(bucket.name, "foo") }, AmazonS3Exception::class.java)
        exception.assertIsNoSuchKey()
    }

    @Test
    fun `get object as string from missing bucket`() {
        val exception = catchThrowableOfType({ testObj.getObjectAsString("missingBucket", "foo") }, AmazonS3Exception::class.java)
        exception.assertIsBucketNotFound()
    }

}