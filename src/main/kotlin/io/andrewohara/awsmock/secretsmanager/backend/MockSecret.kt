package io.andrewohara.awsmock.secretsmanager.backend

import java.nio.ByteBuffer

class MockSecret internal constructor(
        val name: String,
        var description: String?,
        val tags: Map<String, String>?,
        var kmsKeyId: String
) {
    val arn = "arn:mockaws:secretsmanager:region:account-id:$name"
    var deleted: Boolean = false

    private val history = mutableListOf<MockSecretValue>()

    fun latest() = history.lastOrNull()
    fun versions() = history.filter { it.stages.isNotEmpty() }.toList()

    fun add(string: String?, binary: ByteBuffer?): MockSecretValue? {
        val version = MockSecretValue.create(string, binary) ?: return null

        history.reversed().drop(1).firstOrNull()?.makeObsolete()
        history.lastOrNull()?.makePrevious()
        history.add(version)

        return version
    }
}