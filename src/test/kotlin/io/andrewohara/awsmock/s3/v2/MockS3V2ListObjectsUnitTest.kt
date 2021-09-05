package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.ListObjectsResponse
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.S3Object

class MockS3V2ListObjectsUnitTest {

    private val backend = MockS3Backend()
    private val testObj = MockS3V2(backend)
    private val bucket = backend.create("bucket")!!

    @Test
    fun `list objects for bucket that doesn't exist`() {
        assertThatThrownBy {
            testObj.listObjects {
                it.bucket("missingBucket")
            }
        }.isInstanceOf(NoSuchBucketException::class.java)
    }

    @Test
    fun `list objects for empty bucket`() {
        val result = testObj.listObjects {
            it.bucket(bucket.name)
        }

        assertThat(result).isEqualTo(
            ListObjectsResponse.builder()
                .contents(emptyList())
                .build()
        )
    }

    @Test
    fun `list objects for bucket`() {
        bucket["obj1"] = "foo"
        bucket["obj2"] = "bar"

        val result = testObj.listObjects {
            it.bucket(bucket.name)
        }

        assertThat(result).isEqualTo(
            ListObjectsResponse.builder()
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

        val result = testObj.listObjects {
            it.bucket(bucket.name)
            it.prefix("foo")
        }

        assertThat(result).isEqualTo(
            ListObjectsResponse.builder()
                .prefix("foo")
                .contents(
                    S3Object.builder().key("foo/obj1").build(),
                    S3Object.builder().key("foo/obj2").build()
                )
                .build()
        )
    }

    @Test
    fun `list objects with key limit`() {
        bucket["foo"] = "bar"
        bucket["toll"] = "troll"

        val result = testObj.listObjects {
            it.bucket(bucket.name)
            it.maxKeys(1)
        }

        assertThat(result).isEqualTo(
            ListObjectsResponse.builder()
                .maxKeys(1)
                .contents(
                    S3Object.builder().key("foo").build()
                )
                .build()
        )
    }
}