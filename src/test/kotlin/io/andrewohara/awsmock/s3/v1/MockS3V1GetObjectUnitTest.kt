package io.andrewohara.awsmock.s3.v1

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.GetObjectRequest
import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import io.andrewohara.awsmock.s3.v1.S3Assertions.assertIsBucketNotFound
import io.andrewohara.awsmock.s3.v1.S3Assertions.assertIsNoSuchKey
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class MockS3V1GetObjectUnitTest {

    private val backend = MockS3Backend()
    private val testObj = MockS3V1(backend)
    private val bucket = backend.create("bucket")!!

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
        bucket["foo"] = "bar"

        val dest = Files.createTempFile("foo", ".txt").toFile()
        dest.deleteOnExit()

        testObj.getObject(GetObjectRequest(bucket.name, "foo"), dest)
        assertThat(dest.readText()).isEqualTo("bar")

        dest.delete()
    }

    // as string

    @Test
    fun `get object as string`() {
        bucket["foo"] = "bar"

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