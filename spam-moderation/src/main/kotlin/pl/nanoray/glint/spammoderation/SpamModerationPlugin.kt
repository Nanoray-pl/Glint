package pl.nanoray.glint.spammoderation

import net.dv8tion.jda.api.JDA
import pl.nanoray.glint.ConfigManager
import pl.nanoray.glint.getConfig
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.dependency.register
import pl.shockah.unikorn.dependency.resolve

class SpamModerationPlugin(
	container: Container
): ContainerEnabledPlugin(container) {
	private val jda: JDA by resolver.inject()

	private val pluginContainer = Container(resolver)
	private val discordModerationWorker: DiscordModerationWorker by pluginContainer.inject()

	private val eventListener by lazy { DiscordEventListener(pluginContainer) }

	init {
		pluginContainer.register { it.resolve<ConfigManager>().getConfig<Config>() ?: throw IllegalArgumentException("Cannot parse Config.") }
		pluginContainer.register<DiscordModerationWorker> { DiscordModerationWorkerImpl(jda) }
		jda.addEventListener(eventListener)
	}

	override fun onUnload() {
		jda.removeEventListener(eventListener)
		super.onUnload()
	}
}