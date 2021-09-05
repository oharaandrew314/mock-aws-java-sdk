package io.andrewohara.awsmock.s3.v1

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockS3V1PresignedUrlUnitTest {

    private val backend = MockS3Backend()
    private val testObj = MockS3V1(backend)
    private val bucket = backend.create("bucket")!!

    @Test
    fun `generate pre-signed get url for bucket that doesn't exist`() {
        val url = testObj.generatePresignedUrl("missingBucket", "foo", null)
        assertThat(url).isNotNull
    }

    @Test
    fun `generate pre-signed get url for object that doesn't exist`() {
        val url = testObj.generatePresignedUrl(bucket.name, "foo", null)
        assertThat(url).isNotNull
    }

    @Test
    fun `generate pre-signed GET url for object`() {
        bucket["foo"] = "bar"

        val url = testObj.generatePresignedUrl(bucket.name, "foo", null)
        assertThat(url).isNotNull
    }

    @Test
    fun `generate pre-signed PUT url for object`() {
        bucket["foo"] = "bar"

        val url = testObj.generatePresignedUrl(bucket.name, "foo", null)
        assertThat(url).isNotNull
    }
}