package io.andrewohara.awsmock.samples.s3

import com.amazonaws.services.s3.AmazonS3
import io.andrewohara.awsmock.s3.MockAmazonS3
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

class S3FileProcessorUnitTest {

    private lateinit var s3Client: AmazonS3
    private lateinit var testObj: S3FileProcessor

    @Before
    fun setup() {
        s3Client = MockAmazonS3()
        val src = s3Client.createBucket("sourceBucket")
        val dest = s3Client.createBucket("catsBucket")

        s3Client.putObject(src.name, "file1.text", "A boring story about a fish")
        s3Client.putObject(src.name, "file2.txt", "An awesome story about a kitty cat!")
        s3Client.putObject(src.name, "file3.txt", "A mildly interesting story about a sheep doggo")
        s3Client.putObject(src.name, "file4.txt", "A heartwarming story about Toggles the cat!")
        s3Client.putObject(src.name, "file5.txt", "It's Official: Cats are incredible!")
        s3Client.putObject(src.name, "file6.txt", "Pet Gerbil chews its way out of cage and defecates on floor")

        testObj = S3FileProcessor(src.name, dest.name, s3Client)
    }

    @Test
    fun `process files`() {
        val numCatStories = testObj.process()

        assertThat(numCatStories).isEqualTo(4)
        assertThat(s3Client.listObjectsV2("sourceBucket").keyCount).isEqualTo(0)
        assertThat(s3Client.listObjectsV2("catsBucket").objectSummaries.map { it.key })
                .containsExactlyInAnyOrder("cat1", "cat2", "cat3", "cat4")
    }
}