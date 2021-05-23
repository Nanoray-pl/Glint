@file:UseSerializers(DurationSerializer::class)

package pl.nanoray.glint.voicetextchannel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import pl.nanoray.glint.jdaextensions.TextChannelIdentifier
import pl.nanoray.glint.jdaextensions.VoiceChannelIdentifier
import pl.nanoray.glint.utilities.DurationSerializer
import kotlin.time.Duration

@Serializable
class ChannelMapping(
	val voiceChannel: VoiceChannelIdentifier,
	val textChannel: TextChannelIdentifier,
	val historyDuration: Duration
)