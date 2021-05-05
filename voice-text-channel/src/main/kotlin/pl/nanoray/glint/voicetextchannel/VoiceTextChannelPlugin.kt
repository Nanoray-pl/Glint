package pl.nanoray.glint.voicetextchannel

import net.dv8tion.jda.api.JDA
import pl.nanoray.glint.DurationParser
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.dependency.register

class VoiceTextChannelPlugin(
		container: Container
): ContainerEnabledPlugin(container) {
	private val jda: JDA by resolver.inject()
	private val durationParser: DurationParser by resolver.inject()

	private val pluginContainer = Container(resolver)
	private val eventListener by lazy { DiscordEventListener(pluginContainer) }

	init {
		println("LOADED PLUGIN")
		register<WritableVoiceTextChannelManager> { VoiceTextChannelManagerImpl(it) }
		pluginContainer.register<DiscordWorker> { DiscordWorkerImpl(it) }
		pluginContainer.register<CommandManager> { CommandManagerImpl(it) }

		jda.addEventListener(eventListener)
	}

	override fun onUnload() {
		jda.removeEventListener(eventListener)
	}
}