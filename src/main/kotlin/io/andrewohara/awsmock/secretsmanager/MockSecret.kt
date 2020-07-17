package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.services.secretsmanager.model.SecretVersionsListEntry
import com.amazonaws.services.secretsmanager.model.Tag
import java.nio.ByteBuffer

class MockSecret(
        val name: String,
        var description: String?,
        val tags: List<Tag>?,
        keyId: String?,
        contentString: String?,
        contentBinary: ByteBuffer?
) {
    var kmsKeyId = keyId ?: "defaultKey"
    val arn = "arn:mockaws:secretsmanager:region:account-id:$name"
    var deleted: Boolean = false

    private val history = mutableListOf(MockSecretValue(string = contentString, binary = contentBinary))

    fun latest() = history.last()

    fun previous() = if (history.size > 1) history[history.size - 2] else null

    fun insert(binary: ByteBuffer?, string: String?): MockSecretValue {
        val latest = MockSecretValue(binary = binary, string = string)
        history.add(latest)
        return latest
    }

    fun update(string: String? = null, binary: ByteBuffer? = null, description: String?, kmsKeyId: String?) {
        latest().update(string, binary)
        if (description != null) this.description = description
        if (kmsKeyId != null) this.kmsKeyId = kmsKeyId
    }

    fun secretVersionListEntries(take: Int? = null): Collection<SecretVersionsListEntry> {
        val entries = mutableListOf(
                SecretVersionsListEntry().withVersionId(latest().version).withVersionStages(listOf("AWSCURRENT"))
        )
        for (previous in history.reversed().drop(1).takeLast(take ?: Int.MAX_VALUE)) {
            entries.add(
                    SecretVersionsListEntry().withVersionId(previous.version).withVersionStages(listOf("AWSPREVIOUS"))
            )
        }

        return entries
    }
}