package pl.nanoray.glint.slashcommand

import net.dv8tion.jda.api.JDA
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.plugin.Plugin
import pl.shockah.unikorn.plugin.PluginInfo

class SlashCommandPlugin(
		container: Container
): ContainerEnabledPlugin(container), SlashCommandProvider {
	private val jda: JDA by resolver.inject()
	private val slashCommandManger: SlashCommandManager by resolver.inject()

	override val globalSlashCommands: Set<SlashCommand> = setOf(PluginsSlashCommand(resolver))
	private val eventListener by lazy { DiscordEventListener(container) }

	init {
		register<SlashCommandDataParser> { SlashCommandDataParserImpl(it) }
		register<SlashCommandManager> { SlashCommandManagerImpl(it, true) }

		jda.addEventListener(eventListener)
		slashCommandManger.registerSlashCommandProvider(this)
	}

	override fun onUnload() {
		jda.removeEventListener(eventListener)
		slashCommandManger.unregisterSlashCommandProvider(this)
		super.onUnload()
	}

	override fun onPluginLoadCycleFinished(allLoadedPlugins: Map<PluginInfo, Plugin>, newlyLoadedPlugins: Map<PluginInfo, Plugin>) {
		super.onPluginLoadCycleFinished(allLoadedPlugins, newlyLoadedPlugins)
		if (jda.status == JDA.Status.CONNECTED)
			slashCommandManger.updateAllSlashCommands()
					.subscribe()
	}
}