package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

class MockS3V2DoesObjectExistUnitTest {

    private val backend = MockS3Backend()
    private val bucket = backend.create("bucket")!!
    private val testObj = MockS3V2(backend)

    @Test
    fun `doesObjectExist for existing object`() {
        bucket["foo"] = "bar"

        testObj.headObject {
            it.bucket(bucket.name)
            it.key("foo")
        }
    }

    @Test
    fun `doesObjectExist for missing object`() {
        assertThatThrownBy {
            testObj.headObject {
                it.bucket(bucket.name)
                it.key("foo")
            }
        }.isInstanceOf(NoSuchKeyException::class.java)
    }

    @Test
    fun `doesObjectExist for missing bucket`() {
        assertThatThrownBy {
            testObj.headObject {
                it.bucket("missingBucket")
                it.key("foo")
            }
        }.isInstanceOf(NoSuchKeyException::class.java)
    }
}