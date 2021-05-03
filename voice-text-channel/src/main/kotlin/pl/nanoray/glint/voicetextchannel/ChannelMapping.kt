package pl.nanoray.glint.voicetextchannel

import pl.nanoray.glint.jdaextensions.TextChannelIdentifier
import pl.nanoray.glint.jdaextensions.VoiceChannelIdentifier
import kotlin.time.Duration

class ChannelMapping(
		val voiceChannel: VoiceChannelIdentifier,
		val textChannel: TextChannelIdentifier,
		configuration: Configuration
) {
	data class Configuration(
			val historyDuration: Duration
	)

	var configuration: Configuration = configuration
		internal set
}