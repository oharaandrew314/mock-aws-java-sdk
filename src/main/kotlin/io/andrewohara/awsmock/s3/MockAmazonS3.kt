package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AbstractAmazonS3
import com.amazonaws.services.s3.model.*
import java.time.Instant
import java.util.*
import java.util.function.Supplier

class MockAmazonS3(
        private val timeProvider: Supplier<Instant> = Supplier { Instant.now() }
): AbstractAmazonS3() {

    private val repo = mutableMapOf<String, MockBucket>()

    override fun createBucket(createBucketRequest: CreateBucketRequest) = createBucket(createBucketRequest.bucketName)

    override fun createBucket(bucketName: String): Bucket {
        // TODO handle already exists case

        repo[bucketName] = MockBucket()

        return Bucket(bucketName).apply {
            creationDate = Date.from(timeProvider.get())
        }
    }

    override fun doesBucketExist(bucketName: String) = doesBucketExistV2(bucketName)
    override fun doesBucketExistV2(bucketName: String?) = repo.containsKey(bucketName)

    override fun putObject(bucketName: String, key: String, content: String): PutObjectResult {
        // TODO handle bucket doesn't exist

        val bucket = repo.getValue(bucketName)
        bucket[key] = MockObject(content.toByteArray(), "text/plain")

        return PutObjectResult()
    }

    override fun doesObjectExist(bucketName: String, objectName: String): Boolean {
        // TODO handle bucket doesn't exist

        val bucket = repo.getValue(bucketName)
        return objectName in bucket
    }

    override fun getObject(bucketName: String, key: String): S3Object {
        // TODO handle bucket doesn't exist

        val bucket = repo.getValue(bucketName)

        // TODO handle object doesn't exist

        val obj = bucket[key]!!

        return S3Object().apply {
            setBucketName(bucketName)
            setKey(key)
            objectMetadata = obj.metadata
            setObjectContent(obj.content.inputStream())
        }
    }

    override fun getObject(getObjectRequest: GetObjectRequest) = getObject(getObjectRequest.bucketName, getObjectRequest.key)

    // List Objects

    override fun listObjects(bucketName: String) = listObjects(bucketName, null)

    override fun listObjects(bucketName: String, prefix: String?): ObjectListing {
        // TODO handle bucket doesn't exist

        val bucket = repo.getValue(bucketName)

        val entries = bucket.objects.keys
                .filter { if (prefix == null) true else it.startsWith(prefix) }
                .map {
                    S3ObjectSummary().apply {
                        this.bucketName = bucketName
                        key = it
                    }
                }

        return ObjectListing().apply {
            setPrefix(prefix)
            setBucketName(bucketName)
            objectSummaries.addAll(entries)
        }
    }
}

private class MockBucket {
    val objects = mutableMapOf<String, MockObject>()

    operator fun set(key: String, obj: MockObject) {
        objects[key] = obj
    }

    operator fun get(key: String) = objects[key]

    operator fun contains(key: String) = key in objects
}

private class MockObject(
        val content: ByteArray,
        val metadata: ObjectMetadata
) {
    constructor(content: ByteArray, contentType: String): this(
            content = content,
            metadata = ObjectMetadata().apply {
                setContentType(contentType)
            }
    )
}