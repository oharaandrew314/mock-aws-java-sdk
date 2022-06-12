package io.andrewohara.awsmock.iot

import java.nio.ByteBuffer

class MockIotDataBackend {

    private val things = mutableMapOf<String, MockIotThing>()

    fun listNamedShadows(thingName: String): List<String>? {
        return things[thingName]?.shadows
            ?.mapNotNull { (key, _) -> key }
    }

    operator fun get(thingName: String, shadowName: String?): ByteBuffer? {
        return things[thingName]?.get(shadowName)
    }
    operator fun set(thingName: String, shadowName: String?, payload: ByteBuffer) {
        val thing = things.getOrPut(thingName) { MockIotThing(thingName) }
        thing[shadowName] = payload
    }
    fun delete(thingName: String, shadowName: String?): ByteBuffer? {
        val thing = things[thingName] ?: return null
        return thing.delete(shadowName)
    }
}

data class MockIotThing(
    val name: String,
    val shadows: MutableMap<String?, ByteBuffer> = mutableMapOf()
) {
    operator fun get(shadowName: String?): ByteBuffer? {
        return shadows[shadowName]
    }

    operator fun set(shadowName: String?, payload: ByteBuffer) {
        shadows[shadowName] = payload
    }

    fun delete(shadowName: String?): ByteBuffer? {
        return shadows.remove(shadowName)
    }
}