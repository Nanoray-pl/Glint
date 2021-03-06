package pl.nanoray.glint.slashcommand

import net.dv8tion.jda.api.JDA
import pl.nanoray.glint.ConfigManager
import pl.nanoray.glint.getConfig
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.plugin.Plugin
import pl.shockah.unikorn.plugin.PluginInfo

class SlashCommandPlugin(
	container: Container
): ContainerEnabledPlugin(container) {
	private val jda: JDA by resolver.inject()
	private val slashCommandManger: SlashCommandManager by resolver.inject()
	private val configManager: ConfigManager by resolver.inject()

	private val eventListener by lazy { DiscordEventListener(container) }
	private val config by lazy { configManager.getConfig() ?: Config() }

	init {
		register<SlashCommandDataParser> { SlashCommandDataParserImpl(it) }
		register<SlashCommandManager> { SlashCommandManagerImpl(it, config.globalCommandsAsGuildCommands) }

		jda.addEventListener(eventListener)
	}

	override fun onUnload() {
		jda.removeEventListener(eventListener)
		super.onUnload()
	}

	override fun onPluginLoadCycleFinished(allLoadedPlugins: Map<PluginInfo, Plugin>, newlyLoadedPlugins: Map<PluginInfo, Plugin>) {
		super.onPluginLoadCycleFinished(allLoadedPlugins, newlyLoadedPlugins)
		if (jda.status == JDA.Status.CONNECTED)
			slashCommandManger.updateAllSlashCommands()
				.subscribe()
	}
}