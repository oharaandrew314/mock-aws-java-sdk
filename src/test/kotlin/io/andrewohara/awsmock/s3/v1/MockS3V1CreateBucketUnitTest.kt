package io.andrewohara.awsmock.s3.v1

import com.amazonaws.services.s3.model.AmazonS3Exception
import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockS3V1CreateBucketUnitTest {

    private val backend = MockS3Backend()
    private val testObj = MockS3V1(backend)

    @Test
    fun `create bucket`() {
        val bucket = testObj.createBucket("bucket")

        assertThat(bucket.name).isEqualTo("bucket")
        assertThat(backend["bucket"]).isNotNull
    }

    @Test
    fun `create bucket that already exists`() {
        val bucket = backend.create("bucket")!!
        bucket["foo"] = "bar"

        assertThatThrownBy {
            testObj.createBucket("bucket")
        }.isInstanceOf(AmazonS3Exception::class.java)
            .hasMessageContaining("already exists")

        assertThat(backend.buckets()).hasSize(1)
    }

    @Test
    fun `create second bucket`() {
        backend.create("bucket1")
        testObj.createBucket("bucket2")

        assertThat(backend.buckets().map { it.name }).containsExactlyInAnyOrder("bucket1", "bucket2")
    }
}