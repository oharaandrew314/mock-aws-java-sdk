package io.andrewohara.awsmock.secretsmanager.backend

import java.nio.ByteBuffer

data class MockSecretValue(
    val versionId: String,
    val string: String?,
    val binary: ByteBuffer?,
    var stages: List<String>
) {
    fun makePrevious() {
        stages = listOf("AWSPREVIOUS")
    }
    fun makeObsolete() {
        stages = listOf()
    }
}