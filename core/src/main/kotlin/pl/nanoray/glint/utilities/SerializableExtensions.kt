package pl.nanoray.glint.utilities

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializer(Regex::class)
object RegexSerializer: KSerializer<Regex> {
	private val stringSerializer = String.serializer()
	override val descriptor = stringSerializer.descriptor

	override fun serialize(encoder: Encoder, value: Regex) {
		stringSerializer.serialize(encoder, value.pattern)
	}

	override fun deserialize(decoder: Decoder): Regex {
		return Regex(stringSerializer.deserialize(decoder))
	}
}