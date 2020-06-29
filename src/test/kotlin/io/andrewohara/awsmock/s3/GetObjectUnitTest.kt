package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.s3.model.GetObjectRequest
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
    fun `get missing object and save it to file`() {
        // TODO
    }

    @Test
    fun `get object and save it to file`() {
        testObj.putObject(bucket.name, "foo", "bar")

        val dest = Files.createTempFile("foo", ".txt")
        dest.toFile().deleteOnExit()
        val request = GetObjectRequest(bucket.name, "foo")

        val metadata = testObj.getObject(request, dest.toFile())
        assertThat(metadata.contentType).isEqualTo("text/plain")
        assertThat(Files.readString(dest)).isEqualTo("bar")
    }
}