package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.S3Exception

class MockS3V2DeleteBucketUnitTest {

    private val backend = MockS3Backend()
    private val testObj = MockS3V2(backend)

    @Test
    fun `delete bucket`() {
        val bucket = backend.create("bucket")!!

        testObj.deleteBucket {
            it.bucket(bucket.name)
        }

        assertThat(backend.buckets()).isEmpty()
    }

    @Test
    fun `delete missing bucket`() {
        assertThatThrownBy {
            testObj.deleteBucket {
                it.bucket("missingBucket")
            }
        }.isInstanceOf(NoSuchBucketException::class.java)
    }

    @Test
    fun `can't delete bucket with objects inside`() {
        val bucket = backend.create("bucket")!!
        bucket["foo"] = "bar"

        assertThatThrownBy {
            testObj.deleteBucket {
                it.bucket(bucket.name)
            }
        }.isInstanceOf(S3Exception::class.java)
            .hasMessageContaining("not empty")
    }
}