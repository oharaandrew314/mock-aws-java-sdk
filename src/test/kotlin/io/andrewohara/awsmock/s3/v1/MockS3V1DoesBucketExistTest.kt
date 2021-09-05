package io.andrewohara.awsmock.s3.v1

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MockS3V1DoesBucketExistTest {

    private val backend = MockS3Backend()
    private val client = MockS3V1(backend)

    @Test
    fun `does bucket exist - missing`() {
        assertThat(client.doesBucketExist("foo")).isFalse
        assertThat(client.doesBucketExistV2("foo")).isFalse
    }

    @Test
    fun `does bucket exist - present`() {
        val bucket = backend.create("foo")!!

        assertThat(client.doesBucketExist(bucket.name)).isTrue
        assertThat(client.doesBucketExistV2(bucket.name)).isTrue
    }
}