package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.NoSuchBucketException

class MockS3V2PutObjectUnitTest {

    private val backend = MockS3Backend()
    private val bucket = backend.create("bucket")!!
    private val testObj = MockS3V2(backend)

    @Test
    fun `put object into missing bucket`() {
        assertThatThrownBy {
            testObj.putObject({
                it.bucket("missingBucket")
                it.key("foo")
            }, RequestBody.fromString("foo"))
        }.isInstanceOf(NoSuchBucketException::class.java)
    }

    @Test
    fun `put object`() {
        testObj.putObject({
            it.bucket(bucket.name)
            it.key("foo")
        }, RequestBody.fromString("bar"))

        assertThat(bucket.getString("foo")).isEqualTo("bar")
    }

    @Test
    fun `replace existing object`() {
        bucket["foo"] = "bar"

        testObj.putObject({
            it.bucket(bucket.name)
            it.key("foo")
        }, RequestBody.fromString("baz"))

        assertThat(bucket.keys()).hasSize(1)
        assertThat(bucket.getString("foo")).isEqualTo("baz")
    }
}