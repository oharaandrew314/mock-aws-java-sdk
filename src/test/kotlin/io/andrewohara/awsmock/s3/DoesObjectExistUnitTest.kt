package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.Bucket
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class DoesObjectExistUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

    @Test
    fun `doesObjectExist for existing object`() {
        testObj.putObject(bucket.name, "foo", "bar")

        assertThat(testObj.doesObjectExist(bucket.name, "foo")).isTrue()
    }

    @Test
    fun `doesObjectExist for missing object`() {
        assertThat(testObj.doesObjectExist(bucket.name, "foo")).isFalse()
    }

    @Test
    fun `doesObjectExist for missing bucket`() {
        assertThat(testObj.doesObjectExist("missingBucket", "foo")).isFalse()
    }
}