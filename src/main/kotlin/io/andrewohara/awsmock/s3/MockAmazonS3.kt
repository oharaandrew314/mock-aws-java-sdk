package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AbstractAmazonS3
import com.amazonaws.services.s3.internal.AmazonS3ExceptionBuilder
import com.amazonaws.services.s3.model.*
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.*
import java.util.function.Supplier

class MockAmazonS3 @JvmOverloads constructor(
        private val timeProvider: Supplier<Instant> = Supplier { Instant.now() }
): AbstractAmazonS3() {

    private val repo = mutableMapOf<String, MockBucket>()

    override fun listBuckets(listBucketsRequest: ListBucketsRequest): List<Bucket> {
        return repo.values.map { it.toBucket() }
    }

    override fun createBucket(createBucketRequest: CreateBucketRequest): Bucket {
        if (repo.containsKey(createBucketRequest.bucketName)) {
            return repo.getValue(createBucketRequest.bucketName).toBucket()
        }

        val bucket = MockBucket(name = createBucketRequest.bucketName, created = timeProvider.get())
        repo[createBucketRequest.bucketName] = bucket

        return bucket.toBucket()
    }

    override fun doesBucketExist(bucketName: String) = doesBucketExistV2(bucketName)
    override fun doesBucketExistV2(bucketName: String?) = repo.containsKey(bucketName)

    override fun deleteBucket(deleteBucketRequest: DeleteBucketRequest) {
        val bucket = repo[deleteBucketRequest.bucketName] ?: throw createNoSuchBucketException()

        if (bucket.keys().isNotEmpty()) {
            throw AmazonS3ExceptionBuilder().apply {
                errorMessage = "The bucket you tried to delete is not empty"
                errorCode = "BucketNotEmpty"
                statusCode = 409
            }.build()
        }

        repo.remove(deleteBucketRequest.bucketName)
    }

    override fun putObject(bucketName: String, key: String, content: String): PutObjectResult {
        val bytes = content.toByteArray()
        val metadata = ObjectMetadata().apply {
            contentType = "text/plain"
            contentLength = bytes.size.toLong()
        }

        bytes.inputStream().use { stream ->
            return putObject(bucketName, key, stream, metadata)
        }
    }

    override fun putObject(putObjectRequest: PutObjectRequest): PutObjectResult {
        val bucket = repo[putObjectRequest.bucketName] ?: throw createNoSuchBucketException()

        val content = if (putObjectRequest.file != null) {
            putObjectRequest.file.readBytes()
        } else {
            putObjectRequest.inputStream.use { it.readBytes() }
        }

        val metadata = putObjectRequest.metadata.clone().apply {
            contentLength = content.size.toLong()
            contentType = contentType ?: putObjectRequest.file?.let { Files.probeContentType(it.toPath()) }
        }

        bucket[putObjectRequest.key] = MockObject(content = content, metadata = metadata)

        return PutObjectResult().apply {
            this.metadata = metadata
        }
    }

    // does object exist

    override fun doesObjectExist(bucketName: String, objectName: String): Boolean {
        val bucket = repo[bucketName] ?: return false
        return objectName in bucket
    }

    // get object

    override fun getObject(getObjectRequest: GetObjectRequest): S3Object {
        val bucket = repo[getObjectRequest.bucketName] ?: throw createNoSuchBucketException()

        val obj = bucket[getObjectRequest.key] ?: throw createNoSuchKeyException()

        return S3Object().apply {
            bucketName = getObjectRequest.bucketName
            key = getObjectRequest.key
            objectMetadata = obj.metadata
            setObjectContent(obj.content.inputStream())
        }
    }

    override fun getObject(getObjectRequest: GetObjectRequest, destinationFile: File): ObjectMetadata {
        val result = getObject(getObjectRequest)
        result.objectContent.use {
            Files.copy(it, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        return result.objectMetadata
    }

    override fun getObjectAsString(bucketName: String, key: String): String {
        getObject(bucketName, key).objectContent.use {
            return it.reader().readText()
        }
    }

    override fun deleteObject(deleteObjectRequest: DeleteObjectRequest) {
        val bucket = repo[deleteObjectRequest.bucketName] ?: throw createNoSuchBucketException()

        bucket.remove(deleteObjectRequest.key)
    }

    override fun listObjects(listObjectsRequest: ListObjectsRequest): ObjectListing {
        val bucket = repo[listObjectsRequest.bucketName] ?: throw createNoSuchBucketException()
        val prefix = listObjectsRequest.prefix

        val summaries = bucket.keys()
                .filter { if (prefix == null) true else it.startsWith(prefix) }
                .map {
                    S3ObjectSummary().apply {
                        this.bucketName = listObjectsRequest.bucketName
                        key = it
                    }
                }
                .take(listObjectsRequest.maxKeys ?: Int.MAX_VALUE)

        return ObjectListing().apply {
            this.prefix = listObjectsRequest.prefix
            this.bucketName = listObjectsRequest.bucketName
            this.objectSummaries.addAll(summaries)
        }
    }

    override fun listObjectsV2(listObjectsV2Request: ListObjectsV2Request): ListObjectsV2Result {
        val request = listObjectsV2Request.let {
            ListObjectsRequest(it.bucketName, it.prefix, null, it.delimiter, it.maxKeys)
        }

        val v1 = listObjects(request)
        return ListObjectsV2Result().apply {
            bucketName = v1.bucketName
            objectSummaries.addAll(v1.objectSummaries)
            prefix = v1.prefix
            keyCount = v1.objectSummaries.size
        }
    }

    override fun deleteObjects(deleteObjectsRequest: DeleteObjectsRequest): DeleteObjectsResult {
        val bucket = repo[deleteObjectsRequest.bucketName] ?: throw createNoSuchBucketException()

        val deleted = deleteObjectsRequest.keys
                .filter { bucket.remove(it.key) != null }
                .map { DeleteObjectsResult.DeletedObject().apply {
                    this.key = it.key
                } }

        return DeleteObjectsResult(deleted)
    }

    override fun generatePresignedUrl(generatePresignedUrlRequest: GeneratePresignedUrlRequest): URL {
        return URL("https", "${generatePresignedUrlRequest.bucketName}.s3.aws.fake", generatePresignedUrlRequest.key)
    }

    override fun copyObject(copyObjectRequest: CopyObjectRequest): CopyObjectResult {
        val srcBucket = repo[copyObjectRequest.sourceBucketName] ?: throw createNoSuchBucketException()
        val destBucket = repo[copyObjectRequest.destinationBucketName] ?: throw createNoSuchBucketException()

        val srcObject = srcBucket[copyObjectRequest.sourceKey] ?: throw createNoSuchKeyException()

        destBucket[copyObjectRequest.destinationKey] = srcObject.copy()

        return CopyObjectResult()
    }

    override fun getObjectMetadata(getObjectMetadataRequest: GetObjectMetadataRequest): ObjectMetadata {
        val bucket = repo[getObjectMetadataRequest.bucketName] ?: throw createNoSuchBucketException()

        val obj = bucket[getObjectMetadataRequest.key] ?: throw createObjectNotFoundException()

        return obj.metadata
    }

    private fun createNoSuchBucketException(): AmazonS3Exception {
        val extendedRequestId = UUID.randomUUID().toString()
        return AmazonS3ExceptionBuilder().apply {
                errorMessage = "The specified bucket does not exist"
                statusCode = 404
                errorCode = "NoSuchBucket"
                requestId = extendedRequestId.split("-").last()
                setExtendedRequestId(extendedRequestId)
            }
            .build()
    }

    private fun createObjectNotFoundException(): AmazonS3Exception {
        val extendedRequestId = UUID.randomUUID().toString()
        return AmazonS3ExceptionBuilder().apply {
                errorMessage = "Not Found"
                errorCode = "404 Not Found"
                statusCode = 404
                requestId = extendedRequestId.split("-").last()
                setExtendedRequestId(extendedRequestId)
            }
            .build()
    }

    private fun createNoSuchKeyException(): AmazonS3Exception {
        val extendedRequestId = UUID.randomUUID().toString()
        return AmazonS3ExceptionBuilder().apply {
            errorMessage = "The specified key does not exist"
            errorCode = "NoSuchKey"
            statusCode = 404
            requestId = extendedRequestId.split("-").last()
            setExtendedRequestId(extendedRequestId)
        }
                .build()
    }
}
