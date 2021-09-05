package io.andrewohara.awsmock.s3

import java.time.Clock

class MockS3Backend @JvmOverloads constructor(private val clock: Clock = Clock.systemUTC()) {

    private val buckets = mutableListOf<MockS3Bucket>()
    fun buckets() = buckets.toList()

    operator fun get(name: String) = buckets.find { it.name == name }

    fun create(name: String): MockS3Bucket? {
        if (get(name) != null) return null

        return MockS3Bucket(name = name, created = clock.instant())
            .also { buckets += it }
    }

    fun delete(bucketName: String): DeleteResult {
        val bucket = get(bucketName) ?: return DeleteResult.NotFound

        if (bucket.keys().isNotEmpty()) return DeleteResult.NotEmpty

        buckets.remove(bucket)
        return DeleteResult.Ok
    }
}

enum class DeleteResult { Ok, NotEmpty, NotFound }