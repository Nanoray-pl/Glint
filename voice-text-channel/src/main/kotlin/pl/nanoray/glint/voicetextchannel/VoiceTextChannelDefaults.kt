package pl.nanoray.glint.voicetextchannel

import java.util.concurrent.TimeUnit
import kotlin.time.toDuration

object VoiceTextChannelDefaults {
	val historyDuration = 15.toDuration(TimeUnit.MINUTES)
}