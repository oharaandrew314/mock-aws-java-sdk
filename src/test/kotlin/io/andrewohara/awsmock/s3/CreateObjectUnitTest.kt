package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class CreateObjectUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

    @Test
    fun `put object into missing bucket`() {
        // TODO
    }

    @Test
    fun `put object with string content`() {
        testObj.putObject(bucket.name, "foo", "bar")

        assertThat(testObj.doesObjectExist(bucket.name, "foo")).isTrue()
    }

    @Test
    fun `put object from file`() {
        // TODO
    }

    @Test
    fun `put object with json content type`() {
        // TODO
    }

    @Test
    fun `put object via inputStream`() {
        // TODO
    }

    @Test
    fun `missing object does not exist`() {
        assertThat(testObj.doesObjectExist(bucket.name, "foo")).isFalse()
    }

    @Test
    fun `find if object exists in missing bucket`() {
        // TODO
    }

    @Test
    fun `get missing object content`() {
        // TODO
    }

    @Test
    fun `get content for object in missing bucket`() {
        // TODO
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
}