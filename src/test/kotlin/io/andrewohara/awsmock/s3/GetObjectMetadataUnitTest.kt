package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.Bucket
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class GetObjectMetadataUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

    @Test
    fun `get metadata for object in missing bucket`() {
        // TODO
    }

    @Test
    fun `get metadata for missing object`() {
        // TODO
    }

    @Test
    fun `get metadata`() {
        testObj.putObject(bucket.name, "foo", "bar")

        val result = testObj.getObjectMetadata(bucket.name, "foo")
        assertThat(result.contentType).isEqualTo("text/plain")
        assertThat(result.contentLength).isEqualTo(3L)
    }
}