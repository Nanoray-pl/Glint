package pl.nanoray.glint.voicetextchannel

import kotlin.time.DurationUnit
import kotlin.time.toDuration

object VoiceTextChannelDefaults {
	val historyDuration = 15.toDuration(DurationUnit.MINUTES)
}