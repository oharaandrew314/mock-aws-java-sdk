package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import io.andrewohara.awsmock.s3.S3Assertions.assertIsBucketNotFound
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test
import java.nio.file.Files

class PutObjectUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

    @Test
    fun `put object into missing bucket`() {
        val exception = catchThrowableOfType({ testObj.putObject("missingBucket", "foo", "bar") }, AmazonS3Exception::class.java)
        exception.assertIsBucketNotFound()
    }

    @Test
    fun `put object with string content`() {
        val result = testObj.putObject(bucket.name, "foo", "bar")

        assertThat(result.metadata.contentType).isEqualTo("text/plain")
        assertThat(testObj.doesObjectExist(bucket.name, "foo")).isTrue()
    }

    @Test
    fun `put json object from file`() {
        val src = Files.createTempFile("foo", ".json")
        src.toFile().deleteOnExit()
        Files.writeString(src, "bar")

        val result = testObj.putObject(bucket.name, "foo", src.toFile())

        assertThat(result.metadata.contentType).isEqualTo("application/json")
    }

    @Test
    fun `put text object from file`() {
        val src = Files.createTempFile("foo", ".txt")
        src.toFile().deleteOnExit()
        Files.writeString(src, "bar")

        val result = testObj.putObject(bucket.name, "foo", src.toFile())

        assertThat(result.metadata.contentType).isEqualTo("text/plain")
    }

    @Test
    fun `put object with json content type`() {
        val metadata = ObjectMetadata().apply {
            contentType = "application/json"
        }

        """{"data":"foo"}""".byteInputStream().use { stream ->
            val result = testObj.putObject(bucket.name, "foo", stream, metadata)
            assertThat(result.metadata.contentType).isEqualTo("application/json")
        }
    }

    @Test
    fun `put text via inputStream`() {
        val metadata = ObjectMetadata().apply {
            contentType = "text/plain"
        }

        "bar".byteInputStream().use { stream ->
            val result = testObj.putObject(bucket.name, "foo", stream, metadata)
            assertThat(result.metadata.contentType).isEqualTo("text/plain")
        }
    }

    @Test
    fun `missing object does not exist`() {
        assertThat(testObj.doesObjectExist(bucket.name, "foo")).isFalse()
    }

    @Test
    fun `get content for string object`() {
        testObj.putObject(bucket.name, "foo", "bar")

        val obj = testObj.getObject(bucket.name, "foo")
        assertThat(obj.bucketName).isEqualTo(bucket.name)
        assertThat(obj.key).isEqualTo("foo")
        assertThat(obj.objectMetadata.contentType).isEqualTo("text/plain")
        assertThat(obj.objectContent.use { String(it?.readAllBytes()!!) }).isEqualTo("bar")
    }

    @Test
    fun `get object with full request object`() {
        testObj.putObject(bucket.name, "toll", "troll")

        val request = GetObjectRequest("bucket", "toll")
        val obj = testObj.getObject(request)

        assertThat(obj.key).isEqualTo("toll")
        assertThat(obj.objectContent.use { String(it?.readAllBytes()!!) }).isEqualTo("troll")
    }

    @Test
    fun `replace existing object content with same content type`() {
        testObj.putObject(bucket.name, "foo", "bar")

        testObj.putObject(bucket.name, "foo", "baz")

        assertThat(testObj.listObjectsV2(bucket.name).keyCount).isEqualTo(1)
        testObj.getObject(bucket.name, "foo").let { updated ->
            assertThat(updated.objectMetadata.contentType).isEqualTo("text/plain")
            assertThat(updated.objectContent.reader().readText()).isEqualTo("baz")
        }
    }

    @Test
    fun `replace existing object content with new content type`() {
        testObj.putObject(bucket.name, "foo", "bar")

        val metadata = ObjectMetadata().apply {
            contentType = "application/json"
        }
        testObj.putObject(bucket.name, "foo", """{"data":"bar"}""".byteInputStream(), metadata)


        assertThat(testObj.listObjectsV2(bucket.name).keyCount).isEqualTo(1)
        testObj.getObject(bucket.name, "foo").let { updated ->
            assertThat(updated.objectMetadata.contentType).isEqualTo("application/json")
            assertThat(updated.objectContent.reader().readText()).isEqualTo("""{"data":"bar"}""")
        }
    }

    @Test
    fun `putObject by stream with missing content length will be aut-filled in response`() {
        val bytes = "bar".toByteArray()
        val metadata = ObjectMetadata().apply {
            contentType = "text/plain"
        }

        bytes.inputStream().use { stream ->
            val result = testObj.putObject(bucket.name, "foo", stream, metadata)
            assertThat(result.metadata.contentLength).isEqualTo(3L)
        }

        assertThat(testObj.getObject(bucket.name, "foo").objectMetadata.contentLength).isEqualTo(3L)
    }
}