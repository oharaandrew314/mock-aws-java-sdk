package io.andrewohara.awsmock.ssm.backend

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class MockSsmBackendTest {

    private val backend = MockSsmBackend()

    @Test
    fun `overwrite secure string with new key`() {
        backend.secure("foo", "bar")
        backend.secure("foo", "baz", keyId = "secretKey")

        backend["foo"]?.history().shouldContainExactly(
            MockSsmParameter.Value(
                type = MockSsmParameter.Type.Secure,
                description = null,
                keyId = "defaultKey",
                version = 1,
                value = "bar"
            ),
            MockSsmParameter.Value(
                type = MockSsmParameter.Type.Secure,
                description = null,
                keyId = "secretKey",
                version = 2,
                value = "baz"
            )
        )
    }

    @Test
    fun `overwrite string to secure string`() {
        backend["foo"] = "bar"
        backend.secure("foo", "baz", keyId = "secretKey")

        backend["foo"]?.history().shouldContainExactly(
            MockSsmParameter.Value(
                type = MockSsmParameter.Type.String,
                description = null,
                keyId = null,
                version = 1,
                value = "bar"
            ),
            MockSsmParameter.Value(
                type = MockSsmParameter.Type.Secure,
                description = null,
                keyId = "secretKey",
                version = 2,
                value = "baz"
            )
        )
    }
}