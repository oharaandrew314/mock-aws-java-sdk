package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.S3Object

class MockS3V2ListObjectsV2UnitTest {

    private val backend = MockS3Backend()
    private val testObj = MockS3V2(backend)
    private val bucket = backend.create("bucket")!!

    @Test
    fun `list objects for bucket that doesn't exist`() {
        assertThatThrownBy {
            testObj.listObjectsV2 {
                it.bucket("missingBucket")
            }
        }.isInstanceOf(NoSuchBucketException::class.java)
    }

    @Test
    fun `list objects for empty bucket`() {
        val result = testObj.listObjectsV2 {
            it.bucket(bucket.name)
        }

        assertThat(result).isEqualTo(
            ListObjectsV2Response.builder()
                .contents(emptyList())
                .keyCount(0)
                .build()
        )
    }

    @Test
    fun `list objects for bucket`() {
        bucket["obj1"] = "foo"
        bucket["obj2"] = "bar"

        val result = testObj.listObjectsV2 {
            it.bucket(bucket.name)
        }

        assertThat(result).isEqualTo(
            ListObjectsV2Response.builder()
                .keyCount(2)
                .contents(
                    S3Object.builder().key("obj1").build(),
                    S3Object.builder().key("obj2").build()
                )
                .build()
        )
    }

    @Test
    fun `list objects with with prefix`() {
        bucket["foo/obj1"] = "bar"
        bucket["foo/obj2"] = "baz"
        bucket["toll/obj"] = "troll"

        val result = testObj.listObjectsV2 {
            it.bucket(bucket.name)
            it.prefix("foo")
        }

        assertThat(result).isEqualTo(
            ListObjectsV2Response.builder()
                .prefix("foo")
                .contents(
                    S3Object.builder().key("foo/obj1").build(),
                    S3Object.builder().key("foo/obj2").build()
                )
                .keyCount(2)
                .build()
        )
    }
}