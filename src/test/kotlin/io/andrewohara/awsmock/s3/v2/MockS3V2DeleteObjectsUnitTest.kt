package io.andrewohara.awsmock.s3.v2

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.DeletedObject
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.ObjectIdentifier

class MockS3V2DeleteObjectsUnitTest {

    private val backend = MockS3Backend()
    private val bucket = backend.create("bucket")!!
    private val testObj = MockS3V2(backend)

    @Test
    fun `delete objects from bucket that doesn't exist`() {
        assertThatThrownBy {
            testObj.deleteObjects { req ->
                req.bucket("missingBucket")
                req.delete { del ->
                    del.objects(
                        ObjectIdentifier.builder().key("toll").build(),
                        ObjectIdentifier.builder().key("spam").build()
                    )
                }
            }
        }.isInstanceOf(NoSuchBucketException::class.java)
    }

    @Test
    fun `delete objects that don't exist`() {
        bucket["foo"] = "bar"

        val resp = testObj.deleteObjects { req ->
            req.bucket(bucket.name)
            req.delete { del ->
                del.objects(
                    ObjectIdentifier.builder().key("toll").build(),
                    ObjectIdentifier.builder().key("spam").build()
                )
            }
        }

        assertThat(resp.deleted()).isEmpty()
    }

    @Test
    fun `delete objects`() {
        bucket["foo"] = "bar"
        bucket["toll"] = "troll"
        bucket["spam"] = "ham"

        val resp = testObj.deleteObjects { req ->
            req.bucket(bucket.name)
            req.delete { del ->
                del.objects(
                    ObjectIdentifier.builder().key("foo").build(),
                    ObjectIdentifier.builder().key("toll").build()
                )
            }
        }

        assertThat(resp.deleted()).containsExactly(
            DeletedObject.builder().key("foo").build(),
            DeletedObject.builder().key("toll").build()
        )

        assertThat(bucket.keys()).containsExactly("spam")
    }
}