package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class DeleteObjectsUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

    @Test
    fun `delete objects from bucket that doesn't exist`() {
        // TODO
    }

    @Test
    fun `delete objects that don't exist`() {
        testObj.putObject(bucket.name, "foo", "bar")

        val request = DeleteObjectsRequest(bucket.name).withKeys("toll", "spam")
        val result = testObj.deleteObjects(request)

        assertThat(result.deletedObjects).isEmpty()
    }

    @Test
    fun `delete objects`() {
        testObj.putObject(bucket.name, "foo", "bar")
        testObj.putObject(bucket.name, "toll", "troll")
        testObj.putObject(bucket.name, "spam", "ham")

        val request = DeleteObjectsRequest(bucket.name).withKeys("foo", "toll")
        val result = testObj.deleteObjects(request)

        assertThat(result.deletedObjects.map { it.key }).containsExactlyInAnyOrder("foo", "toll")
    }
}