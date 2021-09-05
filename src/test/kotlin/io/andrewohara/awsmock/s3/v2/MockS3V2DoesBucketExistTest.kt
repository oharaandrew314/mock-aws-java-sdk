package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.NoSuchBucketException

class MockS3V2DoesBucketExistTest {

    private val backend = MockS3Backend()
    private val client = MockS3V2(backend)

    @Test
    fun `does bucket exist - missing`() {
        assertThatThrownBy {
            client.headBucket {
                it.bucket("missingBucket")
            }
        }.isInstanceOf(NoSuchBucketException::class.java)
    }

    @Test
    fun `does bucket exist - present`() {
        val bucket = backend.create("foo")!!

        client.headBucket {
            it.bucket(bucket.name)
        }
    }
}