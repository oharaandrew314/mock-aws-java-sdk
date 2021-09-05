package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

class MockS3V2CopyObjectUnitTest {

    private val backend = MockS3Backend()
    private val bucket = backend.create("bucket")!!
    private val testObj = MockS3V2(backend)

    @Test
    fun `copy object from bucket that doesn't exist`() {
        assertThatThrownBy {
            testObj.copyObject {
                it.sourceBucket("missingBucket")
                it.sourceKey("foo")
                it.destinationBucket(bucket.name)
                it.destinationKey("foo")
            }
        }.isInstanceOf(NoSuchBucketException::class.java)
    }

    @Test
    fun `copy object to bucket that doesn't exist`() {
        bucket["foo"] = "bar"

        assertThatThrownBy {
            testObj.copyObject {
                it.sourceBucket(bucket.name)
                it.sourceKey("foo")
                it.destinationBucket("missingBucket")
                it.destinationKey("foo")
            }
        }.isInstanceOf(NoSuchBucketException::class.java)
    }

    @Test
    fun `copy object that doesn't exist`() {
        assertThatThrownBy {
            testObj.copyObject {
                it.sourceBucket(bucket.name)
                it.sourceKey("foo")
                it.destinationBucket(bucket.name)
                it.destinationKey("bar")
            }
        }.isInstanceOf(NoSuchKeyException::class.java)
    }

    @Test
    fun `copy object to existing bucket and new key`() {
        bucket["foo"] = "bar"

        testObj.copyObject {
            it.sourceBucket(bucket.name)
            it.sourceKey("foo")
            it.destinationBucket(bucket.name)
            it.destinationKey("toll")
        }

        assertThat(bucket.keys()).hasSize(2)
        assertThat(bucket.getString("foo")).isEqualTo("bar")
        assertThat(bucket.getString("toll")).isEqualTo("bar")
    }

    @Test
    fun `copy object to same bucket - replacing existing object`() {
        bucket["foo"] = "bar"
        bucket["toll"] = "troll"

        testObj.copyObject {
            it.sourceBucket(bucket.name)
            it.sourceKey("foo")
            it.destinationBucket(bucket.name)
            it.destinationKey("toll")
        }

        assertThat(bucket.keys()).hasSize(2)
        assertThat(bucket.getString("foo")).isEqualTo("bar")
        assertThat(bucket.getString("toll")).isEqualTo("bar")
    }

    @Test
    fun `copy object to new bucket`() {
        val bucket2 = backend.create("bucket2")!!
        bucket["foo"] = "bar"

        testObj.copyObject {
            it.sourceBucket(bucket.name)
            it.sourceKey("foo")
            it.destinationBucket(bucket2.name)
            it.destinationKey("foo")
        }

        assertThat(bucket.keys()).hasSize(1)
        assertThat(bucket2.keys()).hasSize(1)
        assertThat(bucket.getString("foo")).isEqualTo("bar")
        assertThat(bucket2.getString("foo")).isEqualTo("bar")
    }

    @Test
    fun `copy object to new bucket, replacing existing object`() {
        val bucket2 = backend.create("bucket2")!!
        bucket["foo"] = "bar"
        bucket2["foo"] = "baz"

        testObj.copyObject {
            it.sourceBucket(bucket.name)
            it.sourceKey("foo")
            it.destinationBucket(bucket2.name)
            it.destinationKey("foo")
        }

        assertThat(bucket.keys()).hasSize(1)
        assertThat(bucket.keys()).hasSize(1)
        assertThat(bucket.getString("foo")).isEqualTo("bar")
        assertThat(bucket2.getString("foo")).isEqualTo("bar")
    }
}