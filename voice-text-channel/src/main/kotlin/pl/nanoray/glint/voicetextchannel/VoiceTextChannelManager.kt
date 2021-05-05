package pl.nanoray.glint.voicetextchannel

import io.reactivex.rxjava3.core.Completable
import pl.nanoray.glint.jdaextensions.TextChannelIdentifier
import pl.nanoray.glint.jdaextensions.VoiceChannelIdentifier
import pl.nanoray.glint.utilities.WithDefault

interface VoiceTextChannelManagerObserver {
	fun onVoiceTextChannelMappingAdded(manager: VoiceTextChannelManager, mapping: ChannelMapping) { }
	fun onVoiceTextChannelMappingRemoved(manager: VoiceTextChannelManager, mapping: ChannelMapping) { }
}

interface VoiceTextChannelManager {
	fun addVoiceTextChannelObserver(observer: VoiceTextChannelManagerObserver)
	fun removeVoiceTextChannelObserver(observer: VoiceTextChannelManagerObserver)

	fun getVoiceTextChannelMappings(): Set<ChannelMapping>
	fun getMappingForVoiceChannel(voiceChannel: VoiceChannelIdentifier): ChannelMapping?
	fun getMappingForTextChannel(textChannel: TextChannelIdentifier): ChannelMapping?
}

interface WritableVoiceTextChannelManager: VoiceTextChannelManager {
	fun linkTextChannelToVoiceChannel(
			textChannel: TextChannelIdentifier,
			voiceChannel: VoiceChannelIdentifier,
			configuration: WithDefault<ChannelMapping.Configuration>
	): Completable

	fun unlinkTextChannelFromVoiceChannel(textChannel: TextChannelIdentifier): Completable
	fun unlinkVoiceChannelFromTextChannel(voiceChannel: VoiceChannelIdentifier): Completable
}