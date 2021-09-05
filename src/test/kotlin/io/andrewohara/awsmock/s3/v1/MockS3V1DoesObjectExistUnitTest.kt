package io.andrewohara.awsmock.s3.v1

import io.andrewohara.awsmock.s3.MockS3Backend
import io.andrewohara.awsmock.s3.MockS3V1
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockS3V1DoesObjectExistUnitTest {

    private val backend = MockS3Backend()
    private val bucket = backend.create("bucket")!!
    private val testObj = MockS3V1(backend)

    @Test
    fun `doesObjectExist for existing object`() {
        bucket["foo"] = "bar"

        assertThat(testObj.doesObjectExist(bucket.name, "foo")).isTrue
    }

    @Test
    fun `doesObjectExist for missing object`() {
        assertThat(testObj.doesObjectExist(bucket.name, "foo")).isFalse
    }

    @Test
    fun `doesObjectExist for missing bucket`() {
        assertThat(testObj.doesObjectExist("missingBucket", "foo")).isFalse
    }
}