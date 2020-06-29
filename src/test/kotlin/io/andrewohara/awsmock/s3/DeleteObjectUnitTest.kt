package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.Bucket
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class DeleteObjectUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

    @Test
    fun `delete object from missing bucket`() {
        // TODO
    }

    @Test
    fun `delete missing object`() {
        // TODO
    }

    @Test
    fun `delete object`() {
        testObj.putObject(bucket.name, "foo", "bar")

        testObj.deleteObject(bucket.name, "foo")
        assertThat(testObj.doesObjectExist(bucket.name, "foo")).isFalse()
    }
}