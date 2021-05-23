package pl.nanoray.glint.voicetextchannel

import io.reactivex.rxjava3.core.Completable
import pl.nanoray.glint.jdaextensions.TextChannelIdentifier
import pl.nanoray.glint.jdaextensions.VoiceChannelIdentifier
import pl.nanoray.glint.utilities.WithDefault
import javax.annotation.CheckReturnValue
import kotlin.time.Duration

interface VoiceTextChannelManagerObserver {
	fun onVoiceTextChannelMappingAdded(manager: VoiceTextChannelManager, mapping: ChannelMapping) { }
	fun onVoiceTextChannelMappingRemoved(manager: VoiceTextChannelManager, mapping: ChannelMapping) { }
}

interface VoiceTextChannelManager {
	val voiceTextChannelMappings: Set<ChannelMapping>

	fun addVoiceTextChannelObserver(observer: VoiceTextChannelManagerObserver)
	fun removeVoiceTextChannelObserver(observer: VoiceTextChannelManagerObserver)

	fun getMappingForVoiceChannel(voiceChannel: VoiceChannelIdentifier): ChannelMapping?
	fun getMappingForTextChannel(textChannel: TextChannelIdentifier): ChannelMapping?
}

interface WritableVoiceTextChannelManager: VoiceTextChannelManager {
	@CheckReturnValue fun linkTextChannelToVoiceChannel(
		textChannel: TextChannelIdentifier,
		voiceChannel: VoiceChannelIdentifier,
		duration: WithDefault<Duration>
	): Completable

	@CheckReturnValue fun unlinkTextChannelFromVoiceChannel(textChannel: TextChannelIdentifier): Completable
	@CheckReturnValue fun unlinkVoiceChannelFromTextChannel(voiceChannel: VoiceChannelIdentifier): Completable
}