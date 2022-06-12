package io.andrewohara.awsmock.iot.v2

import io.andrewohara.awsmock.iot.MockIotDataBackend
import io.andrewohara.awsmock.iot.MockIotDataV2

class V2UpdateThingShadowTest {

    private val backend = MockIotDataBackend()
    private val client = MockIotDataV2(backend)
}