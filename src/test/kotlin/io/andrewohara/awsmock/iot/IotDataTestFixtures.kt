package io.andrewohara.awsmock.iot

import java.nio.ByteBuffer

object IotDataTestFixtures {
    val payload1 = ByteBuffer.wrap("meow".encodeToByteArray())
    val payload2 = ByteBuffer.wrap("trill".encodeToByteArray())
    val payload3 = ByteBuffer.wrap("purr".encodeToByteArray())
}