package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.Bucket
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class DeleteBucketUnitTest {

    private lateinit var testObj: AmazonS3
    private lateinit var bucket: Bucket

    @Before
    fun setup() {
        testObj = MockAmazonS3()
        bucket = testObj.createBucket("bucket")
    }

    @Test
    fun `delete bucket`() {
        testObj.deleteBucket(bucket.name)
        assertThat(testObj.listBuckets()).isEmpty()
    }

    @Test
    fun `delete missing bucket`() {
        // TODO
    }

    @Test
    fun `can't delete bucket with objects inside`() {
        // TODO
    }
}