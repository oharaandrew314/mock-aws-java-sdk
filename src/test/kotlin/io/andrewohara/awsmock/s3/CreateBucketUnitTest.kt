package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CreateBucketRequest
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class CreateBucketUnitTest {

    private lateinit var testObj: AmazonS3

    @Before
    fun setup() {
        testObj = MockAmazonS3()
    }

    @Test
    fun `create bucket and test if it exists`() {
        val bucket = testObj.createBucket("bucket")
        assertThat(bucket.name).isEqualTo("bucket")

        assertThat(testObj.doesBucketExist("bucket")).isTrue()
        assertThat(testObj.doesBucketExistV2("bucket")).isTrue()
    }

    @Test
    fun `create multiple buckets`() {
        testObj.createBucket("bucket1")
        testObj.createBucket("bucket2")

        assertThat(testObj.doesBucketExistV2("bucket1")).isTrue()
        assertThat(testObj.doesBucketExistV2("bucket1")).isTrue()
    }

    @Test
    fun `missing bucket does not exist`() {
        assertThat(testObj.doesBucketExistV2("bucket")).isFalse()
    }

    @Test
    fun `create bucket with full request`() {
        val request = CreateBucketRequest("mybucket")
        val bucket = testObj.createBucket(request)

        assertThat(bucket.name).isEqualTo("mybucket")
        assertThat(testObj.doesBucketExistV2("mybucket")).isTrue()
    }
}