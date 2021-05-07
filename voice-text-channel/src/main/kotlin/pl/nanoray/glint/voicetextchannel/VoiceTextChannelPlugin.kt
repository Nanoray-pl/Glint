package pl.nanoray.glint.voicetextchannel

import net.dv8tion.jda.api.JDA
import pl.nanoray.glint.DurationParser
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.nanoray.glint.slashcommand.SlashCommand
import pl.nanoray.glint.slashcommand.SlashCommandManager
import pl.nanoray.glint.slashcommand.SlashCommandProvider
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.dependency.register

class VoiceTextChannelPlugin(
		container: Container
): ContainerEnabledPlugin(container), SlashCommandProvider {
	private val jda: JDA by resolver.inject()
	private val durationParser: DurationParser by resolver.inject()
	private val slashCommandManager: SlashCommandManager by resolver.inject()

	private val pluginContainer = Container(resolver)
	private val eventListener by lazy { DiscordEventListener(pluginContainer) }

	override val globalSlashCommands: Set<SlashCommand> = setOf(VoiceTextSlashCommand(resolver))

	init {
		register<WritableVoiceTextChannelManager> { VoiceTextChannelManagerImpl(it) }
		pluginContainer.register<DiscordWorker> { DiscordWorkerImpl(it) }

		jda.addEventListener(eventListener)
		slashCommandManager.registerSlashCommandProvider(this)
	}

	override fun onUnload() {
		jda.removeEventListener(eventListener)
		slashCommandManager.unregisterSlashCommandProvider(this)
		super.onUnload()
	}
}