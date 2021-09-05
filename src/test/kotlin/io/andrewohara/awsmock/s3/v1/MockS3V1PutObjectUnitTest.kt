package io.andrewohara.awsmock.s3.v1

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ObjectMetadata
import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import io.andrewohara.awsmock.s3.v1.S3Assertions.assertIsBucketNotFound
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class MockS3V1PutObjectUnitTest {

    private val backend = MockS3Backend()
    private val bucket = backend.create("bucket")!!
    private val testObj = MockS3V1(backend)

    @Test
    fun `put object into missing bucket`() {
        val exception = catchThrowableOfType({ testObj.putObject("missingBucket", "foo", "bar") }, AmazonS3Exception::class.java)
        exception.assertIsBucketNotFound()
    }

    @Test
    fun `put object with string content`() {
        testObj.putObject(bucket.name, "foo", "bar")

        assertThat(bucket.getString("foo")).isEqualTo("bar")
    }

    @Test
    fun `put object from file`() {
        val src = Files.createTempFile("foo", ".json").toFile()
        src.deleteOnExit()
        src.writeText("bar")

        testObj.putObject(bucket.name, "foo", src)
        src.delete()

        assertThat(bucket.getString("foo")).isEqualTo("bar")
    }

    @Test
    fun `put via inputStream`() {

        "bar".byteInputStream().use { stream ->
            testObj.putObject(bucket.name, "foo", stream, ObjectMetadata())
        }
        assertThat(bucket.getString("foo")).isEqualTo("bar")
    }

    @Test
    fun `replace existing object`() {
        bucket["foo"] = "bar"

        testObj.putObject(bucket.name, "foo", "baz")

        assertThat(bucket.keys()).hasSize(1)
        assertThat(bucket.getString("foo")).isEqualTo("baz")
    }
}