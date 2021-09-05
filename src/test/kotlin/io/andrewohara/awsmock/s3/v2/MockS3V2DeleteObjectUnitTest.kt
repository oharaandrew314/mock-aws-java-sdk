package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.NoSuchBucketException

class MockS3V2DeleteObjectUnitTest {

    private val backend = MockS3Backend()
    private val bucket = backend.create("bucket")!!
    private val testObj = MockS3V2(backend)

    @Test
    fun `delete object from missing bucket`() {
        assertThatThrownBy {
            testObj.deleteObject {
                it.bucket("missingBucket")
                it.key("foo")
            }
        }.isInstanceOf(NoSuchBucketException::class.java)
    }

    @Test
    fun `delete missing object`() {
        // nothing should happen
        testObj.deleteObject {
            it.bucket(bucket.name)
            it.key("foo")
        }
    }

    @Test
    fun `delete object`() {
        bucket["foo"] = "bar"

        testObj.deleteObject {
            it.bucket(bucket.name)
            it.key("foo")
        }
        assertThat(bucket["foo"]).isNull()
    }
}