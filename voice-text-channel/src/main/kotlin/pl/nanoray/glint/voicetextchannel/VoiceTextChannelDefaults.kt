package pl.nanoray.glint.voicetextchannel

import java.util.concurrent.TimeUnit
import kotlin.time.toDuration

object VoiceTextChannelDefaults {
	val mappingConfiguration = ChannelMapping.Configuration(
			historyDuration = 15.toDuration(TimeUnit.MINUTES)
	)
}