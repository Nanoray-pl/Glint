package pl.nanoray.glint.voicetextchannel

import net.dv8tion.jda.api.JDA
import pl.nanoray.glint.DurationParser
import pl.nanoray.glint.command.Command
import pl.nanoray.glint.command.CommandManager
import pl.nanoray.glint.command.CommandProvider
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.dependency.register

class VoiceTextChannelPlugin(
		container: Container
): ContainerEnabledPlugin(container), CommandProvider {
	private val jda: JDA by resolver.inject()
	private val durationParser: DurationParser by resolver.inject()
	private val commandManager: CommandManager by resolver.inject()

	private val pluginContainer = Container(resolver)
	private val eventListener by lazy { DiscordEventListener(pluginContainer) }

	override val globalCommands: Set<Command> = setOf(VoiceTextCommand(resolver))

	init {
		register<WritableVoiceTextChannelManager> { VoiceTextChannelManagerImpl(it) }
		pluginContainer.register<DiscordWorker> { DiscordWorkerImpl(it) }

		jda.addEventListener(eventListener)
		commandManager.registerCommandProvider(this)
	}

	override fun onUnload() {
		jda.removeEventListener(eventListener)
		commandManager.unregisterCommandProvider(this)
		super.onUnload()
	}
}