package pl.nanoray.glint.command

import net.dv8tion.jda.api.JDA
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.plugin.Plugin
import pl.shockah.unikorn.plugin.PluginInfo

class CommandPlugin(
		container: Container
): ContainerEnabledPlugin(container), CommandProvider {
	private val jda: JDA by resolver.inject()
	private val commandManger: CommandManager by resolver.inject()

	override val globalCommands: Set<Command> = setOf(PluginsCommand(resolver))
	private val eventListener by lazy { DiscordEventListener(container) }

	init {
		register<CommandDataParser> { CommandDataParserImpl(it) }
		register<CommandManager> { CommandManagerImpl(it, true) }

		jda.addEventListener(eventListener)
		commandManger.registerCommandProvider(this)
	}

	override fun onUnload() {
		jda.removeEventListener(eventListener)
		commandManger.unregisterCommandProvider(this)
		super.onUnload()
	}

	override fun onPluginLoadCycleFinished(allLoadedPlugins: Map<PluginInfo, Plugin>, newlyLoadedPlugins: Map<PluginInfo, Plugin>) {
		super.onPluginLoadCycleFinished(allLoadedPlugins, newlyLoadedPlugins)
		if (jda.status == JDA.Status.CONNECTED)
			commandManger.updateAllCommands()
					.subscribe()
	}
}