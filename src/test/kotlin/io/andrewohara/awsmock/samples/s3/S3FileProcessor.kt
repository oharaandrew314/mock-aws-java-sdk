package io.andrewohara.awsmock.samples.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder

class S3FileProcessor @JvmOverloads constructor(
        private val sourceBucket: String,
        private val catsBucket: String,
        s3: AmazonS3? = null
) {
    private val s3Client = s3 ?: AmazonS3ClientBuilder.defaultClient()

    fun process(): Int {
        var numCats = 0

        for (header in s3Client.listObjects(sourceBucket).objectSummaries) {
            val content = s3Client.getObjectAsString(sourceBucket, header.key)
            if (content.contains("cat", ignoreCase = true)) {
                s3Client.copyObject(sourceBucket, header.key, catsBucket, "cat${++numCats}")
            }
            s3Client.deleteObject(sourceBucket, header.key)
        }

        return numCats
    }
}