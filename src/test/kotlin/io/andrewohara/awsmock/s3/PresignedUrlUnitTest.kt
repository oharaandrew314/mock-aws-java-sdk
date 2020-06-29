package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.Bucket
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class PresignedUrlUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

    @Test
    fun `generate pre-signed get url for object that doesn't exist`() {
        // TODO
    }

    @Test
    fun `generate pre-signed GET url for object`() {
        testObj.putObject(bucket.name, "foo", "bar")

        val url = testObj.generatePresignedUrl(bucket.name, "foo", null)
        assertThat(url).isNotNull()
    }

    @Test
    fun `generate presigned PUT url for object`() {
        testObj.putObject(bucket.name, "foo", "bar")

        val url = testObj.generatePresignedUrl(bucket.name, "foo", null)
        assertThat(url).isNotNull()
    }
}