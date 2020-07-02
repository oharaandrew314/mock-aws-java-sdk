package io.andrewohara.awsmock.s3

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AbstractAmazonS3
import com.amazonaws.services.s3.internal.AmazonS3ExceptionBuilder
import com.amazonaws.services.s3.model.*
import java.io.File
import java.io.InputStream
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

    // list buckets

    override fun listBuckets(): List<Bucket> {
        val request = ListBucketsRequest()
        return listBuckets(request)
    }

    override fun listBuckets(listBucketsRequest: ListBucketsRequest): List<Bucket> {
        return repo.values.map { it.toBucket() }
    }

    // create bucket

    override fun createBucket(createBucketRequest: CreateBucketRequest): Bucket {
        if (repo.containsKey(createBucketRequest.bucketName)) {
            return repo.getValue(createBucketRequest.bucketName).toBucket()
        }

        val bucket = MockBucket(name = createBucketRequest.bucketName, created = timeProvider.get())
        repo[createBucketRequest.bucketName] = bucket

        return bucket.toBucket()
    }

    override fun createBucket(bucketName: String) = createBucket(bucketName, "us-east-1")

    override fun createBucket(bucketName: String, region: String) = createBucket(bucketName, Region.fromValue(region))

    override fun createBucket(bucketName: String, region: Region): Bucket {
        val request = CreateBucketRequest(bucketName)
        return createBucket(request)
    }

    // does bucket exist

    override fun doesBucketExist(bucketName: String) = doesBucketExistV2(bucketName)
    override fun doesBucketExistV2(bucketName: String?) = repo.containsKey(bucketName)

    // delete bucket

    override fun deleteBucket(bucketName: String) {
        val request = DeleteBucketRequest(bucketName)
        deleteBucket(request)
    }

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

    // put object

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

        val metadata = putObjectRequest.metadata.clone()
        if (metadata.contentLength == 0L) {
            metadata.contentLength = putObjectRequest.inputStream.available().toLong()
        }

        putObjectRequest.inputStream.use { content ->
            bucket[putObjectRequest.key] = MockObject(
                    content = content.readBytes(),
                    metadata = metadata
            )
        }

        return PutObjectResult().apply {
            this.metadata = metadata
        }
    }

    override fun putObject(bucketName: String, key: String, file: File): PutObjectResult {

        val metadata = ObjectMetadata().apply {
            contentType = Files.probeContentType(file.toPath())
            contentLength = Files.size(file.toPath())
        }

        Files.newInputStream(file.toPath()).use { stream ->
            return putObject(bucketName, key, stream, metadata)
        }
    }

    override fun putObject(bucketName: String, key: String, input: InputStream, metadata: ObjectMetadata): PutObjectResult {
        val request = PutObjectRequest(bucketName, key, input, metadata)
        return putObject(request)
    }

    // does object exist

    override fun doesObjectExist(bucketName: String, objectName: String): Boolean {
        val bucket = repo[bucketName] ?: return false
        return objectName in bucket
    }

    // get object

    override fun getObject(bucketName: String, key: String): S3Object {
        val bucket = repo[bucketName] ?: throw createNoSuchBucketException()

        val obj = bucket[key] ?: throw createNoSuchKeyException()

        return S3Object().apply {
            setBucketName(bucketName)
            setKey(key)
            objectMetadata = obj.metadata
            setObjectContent(obj.content.inputStream())
        }
    }

    override fun getObject(getObjectRequest: GetObjectRequest) = getObject(getObjectRequest.bucketName, getObjectRequest.key)

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

    // delete object

    override fun deleteObject(deleteObjectRequest: DeleteObjectRequest) {
        val bucket = repo[deleteObjectRequest.bucketName] ?: throw createNoSuchBucketException()

        bucket.remove(deleteObjectRequest.key)
    }

    override fun deleteObject(bucketName: String, key: String) {
        val request = DeleteObjectRequest(bucketName, key)
        deleteObject(request)
    }

    // List Objects

    override fun listObjects(bucketName: String) = listObjects(bucketName, null)

    override fun listObjects(bucketName: String, prefix: String?): ObjectListing {
        val request = ListObjectsRequest(bucketName, prefix, null, null, null)
        return listObjects(request)
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

    // list objects v2

    override fun listObjectsV2(listObjectsV2Request: ListObjectsV2Request): ListObjectsV2Result {
        val request = listObjectsV2Request.let {
            ListObjectsRequest(it.bucketName, it.prefix, null, it.delimiter, it.maxKeys)
        }

        return listObjects(request).toV2()
    }

    override fun listObjectsV2(bucketName: String, prefix: String?): ListObjectsV2Result {
        val request = ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix)

        return listObjectsV2(request)
    }

    override fun listObjectsV2(bucketName: String) = listObjectsV2(bucketName, null)

    private fun ObjectListing.toV2() = ListObjectsV2Result().let { v2 ->
        v2.bucketName = bucketName
        v2.objectSummaries.addAll(objectSummaries)
        v2.prefix = prefix
        v2.keyCount = objectSummaries.size
        v2
    }

    // delete objects

    override fun deleteObjects(deleteObjectsRequest: DeleteObjectsRequest): DeleteObjectsResult {
        val bucket = repo[deleteObjectsRequest.bucketName] ?: throw createNoSuchBucketException()

        val deleted = deleteObjectsRequest.keys
                .filter { bucket.remove(it.key) != null }
                .map { DeleteObjectsResult.DeletedObject().apply {
                    this.key = it.key
                } }

        return DeleteObjectsResult(deleted)
    }

    // Presigned URLs

    override fun generatePresignedUrl(generatePresignedUrlRequest: GeneratePresignedUrlRequest): URL {
        return URL("https", "${generatePresignedUrlRequest.bucketName}.s3.aws.fake", generatePresignedUrlRequest.key)
    }

    override fun generatePresignedUrl(bucketName: String, key: String, expiration: Date?, method: HttpMethod): URL {
        val request = GeneratePresignedUrlRequest(bucketName, key, method).apply {
            this.expiration = expiration
        }

        return generatePresignedUrl(request)
    }

    override fun generatePresignedUrl(bucketName: String, key: String, expiration: Date?): URL {
        return generatePresignedUrl(bucketName, key, expiration, HttpMethod.GET)
    }

    // copy object

    override fun copyObject(copyObjectRequest: CopyObjectRequest): CopyObjectResult {
        val srcBucket = repo[copyObjectRequest.sourceBucketName] ?: throw createNoSuchBucketException()
        val destBucket = repo[copyObjectRequest.destinationBucketName] ?: throw createNoSuchBucketException()

        val srcObject = srcBucket[copyObjectRequest.sourceKey] ?: throw createNoSuchKeyException()

        destBucket[copyObjectRequest.destinationKey] = srcObject.copy()

        return CopyObjectResult()
    }

    override fun copyObject(sourceBucketName: String, sourceKey: String, destinationBucketName: String, destinationKey: String): CopyObjectResult {
        val request = CopyObjectRequest(sourceBucketName, sourceKey, destinationBucketName, destinationKey)
        return copyObject(request)
    }

    // get object metadata

    override fun getObjectMetadata(getObjectMetadataRequest: GetObjectMetadataRequest): ObjectMetadata {
        val bucket = repo[getObjectMetadataRequest.bucketName] ?: throw createNoSuchBucketException()

        val obj = bucket[getObjectMetadataRequest.key] ?: throw createObjectNotFoundException()

        return obj.metadata
    }

    override fun getObjectMetadata(bucketName: String, key: String): ObjectMetadata {
        val request = GetObjectMetadataRequest(bucketName, key)
        return getObjectMetadata(request)
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
