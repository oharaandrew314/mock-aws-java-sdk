package io.andrewohara.awsmock.s3

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.http.AbortableInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*

class MockS3V2(private val backend: MockS3Backend = MockS3Backend()): S3Client {

    override fun close() {}
    override fun serviceName() = "s3-mock"

    override fun copyObject(copyObjectRequest: CopyObjectRequest): CopyObjectResponse {
        val sourceBucket = backend[copyObjectRequest.sourceBucket()] ?: throw bucketDoesNotExist()
        val content = sourceBucket[copyObjectRequest.sourceKey()] ?: throw keyDoesNotExist()

        val destBucket = backend[copyObjectRequest.destinationBucket()] ?: throw bucketDoesNotExist()
        destBucket[copyObjectRequest.destinationKey()] = content

        return CopyObjectResponse.builder()
            .build()
    }

    override fun createBucket(createBucketRequest: CreateBucketRequest): CreateBucketResponse {
        backend.create(createBucketRequest.bucket())
            ?: throw BucketAlreadyExistsException.builder().message("bucket already exists").build()

        return CreateBucketResponse.builder().build()
    }

    override fun deleteBucket(deleteBucketRequest: DeleteBucketRequest): DeleteBucketResponse {
        return when(backend.delete(deleteBucketRequest.bucket())) {
            DeleteResult.Ok -> DeleteBucketResponse.builder().build()
            DeleteResult.NotFound -> throw bucketDoesNotExist()
            DeleteResult.NotEmpty -> throw S3Exception.builder().message("bucket is not empty").build()
        }
    }

    override fun deleteObject(deleteObjectRequest: DeleteObjectRequest): DeleteObjectResponse {
        val bucket = backend[deleteObjectRequest.bucket()] ?: throw bucketDoesNotExist()

        bucket.remove(deleteObjectRequest.key())

        return DeleteObjectResponse.builder().build()
    }

    override fun deleteObjects(deleteObjectsRequest: DeleteObjectsRequest): DeleteObjectsResponse {
        val bucket = backend[deleteObjectsRequest.bucket()] ?: throw bucketDoesNotExist()

        val deleted = deleteObjectsRequest.delete().objects()
            .filter { bucket.remove(it.key()) }
            .map { DeletedObject.builder().key(it.key()).build() }

        return DeleteObjectsResponse.builder()
            .deleted(deleted)
            .build()
    }

    override fun <ReturnT> getObject(getObjectRequest: GetObjectRequest, responseTransformer: ResponseTransformer<GetObjectResponse, ReturnT>): ReturnT {
        val bucket = backend[getObjectRequest.bucket()] ?: throw bucketDoesNotExist()
        val content = bucket[getObjectRequest.key()] ?: throw keyDoesNotExist()

        val response = GetObjectResponse.builder().build()

        return responseTransformer.transform(response, AbortableInputStream.create(content.inputStream()))
    }

    override fun listBuckets(): ListBucketsResponse {
        val buckets = backend.buckets()
            .map { Bucket.builder().name(it.name).creationDate(it.created).build() }

        return ListBucketsResponse.builder()
            .buckets(buckets)
            .build()
    }

    override fun listObjects(listObjectsRequest: ListObjectsRequest): ListObjectsResponse {
        val bucket = backend[listObjectsRequest.bucket()] ?: throw bucketDoesNotExist()

        val objects = bucket.keys(prefix = listObjectsRequest.prefix(), limit = listObjectsRequest.maxKeys())
            .map { S3Object.builder().key(it).build() }

        return ListObjectsResponse.builder()
            .prefix(listObjectsRequest.prefix())
            .maxKeys(listObjectsRequest.maxKeys())
            .contents(objects)
            .build()
    }

    override fun listObjectsV2(listObjectsV2Request: ListObjectsV2Request): ListObjectsV2Response {
        val bucket = backend[listObjectsV2Request.bucket()] ?: throw bucketDoesNotExist()

        val objects = bucket.keys(prefix = listObjectsV2Request.prefix(), limit = listObjectsV2Request.maxKeys())
            .map { S3Object.builder().key(it).build() }

        return ListObjectsV2Response.builder()
            .prefix(listObjectsV2Request.prefix())
            .maxKeys(listObjectsV2Request.maxKeys())
            .keyCount(objects.size)
            .contents(objects)
            .build()
    }

    override fun putObject(putObjectRequest: PutObjectRequest, requestBody: RequestBody): PutObjectResponse {
        val bucket = backend[putObjectRequest.bucket()] ?: throw bucketDoesNotExist()

        bucket[putObjectRequest.key()] = requestBody.contentStreamProvider().newStream().readBytes()

        return PutObjectResponse.builder()
            .build()
    }

    override fun headBucket(headBucketRequest: HeadBucketRequest): HeadBucketResponse {
        backend[headBucketRequest.bucket()] ?: throw bucketDoesNotExist()

        return HeadBucketResponse.builder()
            .build()
    }

    override fun headObject(headObjectRequest: HeadObjectRequest): HeadObjectResponse {
        val bucket = backend[headObjectRequest.bucket()] ?: throw keyDoesNotExist()
        bucket[headObjectRequest.key()] ?: throw keyDoesNotExist()

        return HeadObjectResponse.builder()
            .build()
    }

    companion object {
        private fun bucketDoesNotExist() = NoSuchBucketException.builder().message("bucket does not exist").build()
        private fun keyDoesNotExist() =  NoSuchKeyException.builder().message("key does not exist").build()
    }
}