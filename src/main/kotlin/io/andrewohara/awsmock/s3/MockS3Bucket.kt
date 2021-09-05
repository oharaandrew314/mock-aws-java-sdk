package io.andrewohara.awsmock.s3

import java.time.Instant

data class MockS3Bucket(val name: String, val created: Instant) {
    private val objects = mutableMapOf<String, ByteArray>()

    operator fun set(key: String, content: ByteArray) {
        objects[key] = content
    }
    operator fun set(key: String, content: String) = set(key, content.toByteArray())

    operator fun get(key: String) = objects[key]
    fun getString(key: String) = get(key)?.decodeToString()

    fun remove(key: String) = objects.remove(key) != null

    fun keys(prefix: String? = null, limit: Int? = null) = objects.keys
        .filter { if (prefix == null) true else it.startsWith(prefix) }
        .take(limit ?: Int.MAX_VALUE)
        .toSet()


    operator fun contains(key: String) = key in objects
}