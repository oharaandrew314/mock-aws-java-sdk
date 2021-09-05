package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException

class MockS3V2CreateBucketUnitTest {

    private val backend = MockS3Backend()
    private val testObj = MockS3V2(backend)

    @Test
    fun `create bucket`() {
        testObj.createBucket {
            it.bucket("bucket")
        }

        assertThat(backend["bucket"]).isNotNull
    }

    @Test
    fun `create bucket that already exists`() {
        val bucket = backend.create("bucket")!!
        bucket["foo"] = "bar"

        assertThatThrownBy {
            testObj.createBucket {
                it.bucket(bucket.name)
            }
        }.isInstanceOf(BucketAlreadyExistsException::class.java)

        assertThat(backend.buckets()).hasSize(1)
    }

    @Test
    fun `create second bucket`() {
        backend.create("bucket1")

        testObj.createBucket {
            it.bucket("bucket2")
        }

        assertThat(backend.buckets().map { it.name }).containsExactlyInAnyOrder("bucket1", "bucket2")
    }
}