package pl.nanoray.glint

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun interface DurationParser {
	fun parseDuration(input: CharSequence): Duration?
}

internal class DefaultDurationParser: DurationParser {
	private val regex = Regex("(-?\\d+(?:\\.\\d+)?)([dhms]|ms)")

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
			else -> return null
		}
		return decimal.toDuration(unit)
	}
}