package pl.nanoray.glint.utilities

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import pl.nanoray.glint.DurationParser
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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

@Serializer(Duration::class)
object DurationSerializer: KSerializer<Duration>, DurationParser {
	private val regex = Regex("(-?(?:\\d+(?:\\.\\d+)?|\\d\\.\\d+e-?\\d+))(ns|us|ms|[smhd])")
	private val stringSerializer = String.serializer()
	override val descriptor = stringSerializer.descriptor

	override fun serialize(encoder: Encoder, value: Duration) {
		stringSerializer.serialize(encoder, value.toString())
	}

	override fun deserialize(decoder: Decoder): Duration {
		return parseDuration(stringSerializer.deserialize(decoder))!!
	}

	override fun parseDuration(input: CharSequence): Duration? {
		val match = regex.matchEntire(input) ?: return null
		val decimal = match.groups[1]?.value?.toDoubleOrNull() ?: return null
		val unitAbbreviation = match.groups[2]?.value ?: return null
		val unit = when (unitAbbreviation) {
			"d" -> DurationUnit.DAYS
			"h" -> DurationUnit.HOURS
			"m" -> DurationUnit.MINUTES
			"s" -> DurationUnit.SECONDS
			"ms" -> DurationUnit.MILLISECONDS
			"us" -> DurationUnit.MICROSECONDS
			"ns" -> DurationUnit.NANOSECONDS
			else -> return null
		}
		return decimal.toDuration(unit)
	}
}