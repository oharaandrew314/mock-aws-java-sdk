package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.Bucket
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MockS3V2ListBucketsUnitTest {

    private val clock = Clock.fixed(Instant.ofEpochSecond(9001), ZoneId.of("UTC"))
    private val backend = MockS3Backend(clock)
    private val testObj = MockS3V2(backend)

    @Test
    fun `list empty buckets`() {
        assertThat(testObj.listBuckets().buckets()).isEmpty()
    }

    @Test
    fun `list buckets`() {
        backend.create("foo")
        backend.create("bar")

        assertThat(testObj.listBuckets().buckets()).containsExactly(
            Bucket.builder().name("foo").creationDate(clock.instant()).build(),
            Bucket.builder().name("bar").creationDate(clock.instant()).build(),
        )
    }
}