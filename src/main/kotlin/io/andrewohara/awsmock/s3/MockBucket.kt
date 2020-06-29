package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.model.Bucket
import java.time.Instant
import java.util.*

data class MockBucket(private val name: String, private val created: Instant) {
    private val objects = mutableMapOf<String, MockObject>()

    operator fun set(key: String, obj: MockObject) {
        objects[key] = obj
    }

    operator fun get(key: String) = objects[key]

    fun remove(key: String): MockObject? = objects.remove(key)

    fun list() = objects.values.toList()

    operator fun contains(key: String) = key in objects

    fun toBucket() = Bucket(name).apply {
        creationDate = Date.from(created)
    }
}