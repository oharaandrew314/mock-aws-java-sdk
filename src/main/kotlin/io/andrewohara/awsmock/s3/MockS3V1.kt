package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AbstractAmazonS3
import com.amazonaws.services.s3.internal.AmazonS3ExceptionBuilder
import com.amazonaws.services.s3.model.*
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

class MockS3V1(private val backend: MockS3Backend = MockS3Backend()): AbstractAmazonS3() {

    override fun listBuckets(listBucketsRequest: ListBucketsRequest): List<Bucket> {
        return backend.buckets().map { it.toBucket() }
    }

    override fun createBucket(createBucketRequest: CreateBucketRequest): Bucket {
        val bucket = backend.create(createBucketRequest.bucketName) ?: throw AmazonS3ExceptionBuilder().also {
            it.errorCode = "BucketAlreadyExists"
            it.errorMessage = "bucket already exists"
            it.statusCode = 409
        }.build()

        return bucket.toBucket()
    }

    private fun MockS3Bucket.toBucket() = Bucket().also {
        it.name = name
        it.creationDate = Date.from(created)
    }

    override fun doesBucketExist(bucketName: String) = doesBucketExistV2(bucketName)
    override fun doesBucketExistV2(bucketName: String) = backend[bucketName] != null

    override fun deleteBucket(deleteBucketRequest: DeleteBucketRequest) {
        when(backend.delete(deleteBucketRequest.bucketName)) {
            DeleteResult.Ok -> {}
            DeleteResult.NotEmpty -> throw AmazonS3ExceptionBuilder().apply {
                errorMessage = "The bucket you tried to delete is not empty"
                errorCode = "BucketNotEmpty"
                statusCode = 409
            }.build()
            DeleteResult.NotFound -> throw createNoSuchBucketException()
        }
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
        val bucket = backend[putObjectRequest.bucketName] ?: throw createNoSuchBucketException()

        bucket[putObjectRequest.key] = if (putObjectRequest.file != null) {
            putObjectRequest.file.readBytes()
        } else {
            putObjectRequest.inputStream.use { it.readBytes() }
        }

        return PutObjectResult()
    }

    // does object exist

    override fun doesObjectExist(bucketName: String, objectName: String): Boolean {
        val bucket = backend[bucketName] ?: return false
        return objectName in bucket
    }

    // get object

    override fun getObject(getObjectRequest: GetObjectRequest): S3Object {
        val bucket = backend[getObjectRequest.bucketName] ?: throw createNoSuchBucketException()
        val obj = bucket[getObjectRequest.key] ?: throw createNoSuchKeyException()

        return S3Object().apply {
            bucketName = getObjectRequest.bucketName
            key = getObjectRequest.key
            setObjectContent(obj.inputStream())
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
        return getObject(bucketName, key).objectContent.use {
            it.reader().readText()
        }
    }

    override fun deleteObject(deleteObjectRequest: DeleteObjectRequest) {
        val bucket = backend[deleteObjectRequest.bucketName] ?: throw createNoSuchBucketException()
        bucket.remove(deleteObjectRequest.key)
    }

    override fun listObjects(listObjectsRequest: ListObjectsRequest): ObjectListing {
        val bucket = backend[listObjectsRequest.bucketName] ?: throw createNoSuchBucketException()

        val summaries = bucket
            .keys(prefix = listObjectsRequest.prefix, limit = listObjectsRequest.maxKeys)
            .map { key ->
                S3ObjectSummary().apply {
                    bucketName = listObjectsRequest.bucketName
                    this.key = key
                }
            }

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
        val bucket = backend[deleteObjectsRequest.bucketName] ?: throw createNoSuchBucketException()

        val deleted = deleteObjectsRequest.keys
            .filter { bucket.remove(it.key) }
            .map { req ->
                DeleteObjectsResult.DeletedObject().apply {
                    key = req.key
                }
            }

        return DeleteObjectsResult(deleted)
    }

    override fun generatePresignedUrl(generatePresignedUrlRequest: GeneratePresignedUrlRequest): URL {
        return URL("https", "${generatePresignedUrlRequest.bucketName}.s3.aws.fake", generatePresignedUrlRequest.key)
    }

    override fun copyObject(copyObjectRequest: CopyObjectRequest): CopyObjectResult {
        val srcBucket = backend[copyObjectRequest.sourceBucketName] ?: throw createNoSuchBucketException()
        val destBucket = backend[copyObjectRequest.destinationBucketName] ?: throw createNoSuchBucketException()

        val srcObject = srcBucket[copyObjectRequest.sourceKey] ?: throw createNoSuchKeyException()
        destBucket[copyObjectRequest.destinationKey] = srcObject

        return CopyObjectResult()
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
