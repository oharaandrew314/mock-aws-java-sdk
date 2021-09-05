package io.andrewohara.awsmock.s3.v1

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockS3V1ListBucketsUnitTest {

    private val backend = MockS3Backend()
    private val testObj = MockS3V1(backend)

    @Test
    fun `list empty buckets`() {
        assertThat(testObj.listBuckets()).isEmpty()
    }

    @Test
    fun `list buckets`() {
        backend.create("foo")
        backend.create("bar")

        assertThat(testObj.listBuckets().map { it.name }).containsExactlyInAnyOrder("foo", "bar")
    }
}