package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class ListBucketsUnitTest {

    private lateinit var testObj: AmazonS3

    @Before
    fun setup() {
        testObj = MockAmazonS3()
    }

    @Test
    fun `list empty buckets`() {
        assertThat(testObj.listBuckets()).isEmpty()
    }

    @Test
    fun `list buckets`() {
        testObj.createBucket("foo")
        testObj.createBucket("bar")

        assertThat(testObj.listBuckets().map { it.name }).containsExactlyInAnyOrder("foo", "bar")
    }
}