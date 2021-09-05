package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

class MockS3V2GetObjectUnitTest {

    private val backend = MockS3Backend()
    private val testObj = MockS3V2(backend)
    private val bucket = backend.create("bucket")!!

    @Test
    fun `get object from missing bucket`() {
        assertThatThrownBy {
            testObj.getObject {
                it.bucket("missingBucket")
                it.key("foo")
            }
        }.isInstanceOf(NoSuchBucketException::class.java)
    }

    @Test
    fun `get missing object`() {
        assertThatThrownBy {
            testObj.getObject {
                it.bucket(bucket.name)
                it.key("foo")
            }
        }.isInstanceOf(NoSuchKeyException::class.java)
    }

    @Test
    fun `get object as stream`() {
        bucket["foo"] = "bar"

        val response = testObj.getObject {
            it.bucket(bucket.name)
            it.key("foo")
        }

        assertThat(response.reader().readText()).isEqualTo("bar")
    }

    @Test
    fun `get object as string`() {
        bucket["foo"] = "bar"

        val transformer = ResponseTransformer<GetObjectResponse, String> { _, inputStream ->
            inputStream.reader().readText()
        }

        val response = testObj.getObject({
            it.bucket(bucket.name)
            it.key("foo")
        }, transformer)

        assertThat(response).isEqualTo("bar")
    }
}