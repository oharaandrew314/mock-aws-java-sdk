package io.andrewohara.awsmock.s3

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AbstractAmazonS3
import com.amazonaws.services.s3.model.*
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.*
import java.util.function.Supplier

class MockAmazonS3(
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

    override fun createBucket(createBucketRequest: CreateBucketRequest) = createBucket(createBucketRequest.bucketName)

    override fun createBucket(bucketName: String): Bucket {
        // TODO handle already exists case

        val bucket = MockBucket(name = bucketName, created = timeProvider.get())

        repo[bucketName] = bucket

        return bucket.toBucket()
    }

    override fun createBucket(bucketName: String, region: Region) = createBucket(bucketName)

    override fun createBucket(bucketName: String, region: String) = createBucket(bucketName)

    // does bucket exist

    override fun doesBucketExist(bucketName: String) = doesBucketExistV2(bucketName)
    override fun doesBucketExistV2(bucketName: String?) = repo.containsKey(bucketName)

    // delete bucket

    override fun deleteBucket(bucketName: String) {
        val request = DeleteBucketRequest(bucketName)
        deleteBucket(request)
    }

    override fun deleteBucket(deleteBucketRequest: DeleteBucketRequest) {
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
        // TODO handle bucket doesn't exist

        val bucket = repo.getValue(putObjectRequest.bucketName)

        putObjectRequest.inputStream.use { content ->
            bucket[putObjectRequest.key] = MockObject(
                    key = putObjectRequest.key,
                    content = content.readAllBytes(),
                    metadata = putObjectRequest.metadata
            )
        }

        return PutObjectResult().apply {
            metadata = putObjectRequest.metadata
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
        // TODO handle bucket doesn't exist

        val bucket = repo.getValue(bucketName)
        return objectName in bucket
    }

    // get object

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

    override fun getObject(getObjectRequest: GetObjectRequest, destinationFile: File): ObjectMetadata {
        // TODO handle object not found

        val result = getObject(getObjectRequest)
        result.objectContent.use {
            Files.copy(it, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        return result.objectMetadata
    }

    override fun getObjectAsString(bucketName: String, key: String): String {
        // TODO handle object not found

        getObject(bucketName, key).objectContent.use {
            return it.reader().readText()
        }
    }

    // delete object

    override fun deleteObject(deleteObjectRequest: DeleteObjectRequest) {
        // TODO handle missing bucket

        val bucket = repo.getValue(deleteObjectRequest.bucketName)

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
        // TODO handle bucket doesn't exist

        val bucket = repo.getValue(listObjectsRequest.bucketName)
        val prefix = listObjectsRequest.prefix

        val summaries = bucket.list()
                .filter { if (prefix == null) true else it.key.startsWith(prefix) }
                .map {
                    S3ObjectSummary().apply {
                        this.bucketName = listObjectsRequest.bucketName
                        key = it.key
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

    override fun listObjectsV2(bucketName: String) = listObjects(bucketName).toV2()

    override fun listObjectsV2(listObjectsV2Request: ListObjectsV2Request): ListObjectsV2Result {
        val request = listObjectsV2Request.let {
            ListObjectsRequest(it.bucketName, it.prefix, null, it.delimiter, it.maxKeys)
        }

        return listObjects(request).toV2()
    }

    override fun listObjectsV2(bucketName: String, prefix: String?) = listObjects(bucketName, prefix).toV2()

    private fun ObjectListing.toV2() = ListObjectsV2Result().let { v2 ->
        v2.bucketName = bucketName
        v2.objectSummaries.addAll(objectSummaries)
        v2.prefix = prefix
        v2.keyCount = objectSummaries.size
        v2
    }

    // delete objects

    override fun deleteObjects(deleteObjectsRequest: DeleteObjectsRequest): DeleteObjectsResult {
        // TODO handle missing bucket

        val bucket = repo.getValue(deleteObjectsRequest.bucketName)

        val deleted = deleteObjectsRequest.keys
                .mapNotNull { bucket.remove(it.key) }
                .map { DeleteObjectsResult.DeletedObject().apply {
                    this.key = it.key
                } }

        return DeleteObjectsResult(deleted)
    }

    // Presigned URLs

    override fun generatePresignedUrl(generatePresignedUrlRequest: GeneratePresignedUrlRequest): URL {
        // TODO handle bucket not found

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
}
