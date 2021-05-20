package pl.nanoray.glint.http

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class JsonHttpBodyEncoder<T> @PublishedApi internal constructor (
		private val jsonFormat: Json,
		private val serializer: KSerializer<T>
): HttpBodyEncoder<T> {
	companion object {
		inline operator fun <reified T> invoke(jsonFormat: Json): JsonHttpBodyEncoder<T> {
			return JsonHttpBodyEncoder(jsonFormat, serializer())
		}
	}

	override fun encodeHttpBody(input: T): HttpBodyEncoder.Result {
		return HttpBodyEncoder.Result("application/json", jsonFormat.encodeToString(serializer, input).toByteArray())
	}
}