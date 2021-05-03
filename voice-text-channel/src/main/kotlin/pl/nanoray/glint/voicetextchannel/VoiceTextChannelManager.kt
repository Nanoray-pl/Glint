package pl.nanoray.glint.voicetextchannel

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

interface MutableVoiceTextChannelManager: VoiceTextChannelManager {
	fun linkTextChannelToVoiceChannel(
			textChannel: TextChannelIdentifier,
			voiceChannel: VoiceChannelIdentifier,
			configuration: WithDefault<ChannelMapping.Configuration>
	)

	fun createLinkedTextChannelForVoiceChannel(
			voiceChannel: VoiceChannelIdentifier,
			configuration: WithDefault<ChannelMapping.Configuration>
	): TextChannelIdentifier

	fun unlinkTextChannelFromVoiceChannel(textChannel: TextChannelIdentifier)
	fun unlinkVoiceChannelFromTextChannel(voiceChannel: VoiceChannelIdentifier)
}