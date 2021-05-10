package pl.nanoray.glint

import kotlin.time.Duration

fun interface DurationParser {
	fun parseDuration(input: CharSequence): Duration?
}