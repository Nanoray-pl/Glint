package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import pl.nanoray.glint.ConfigManager
import pl.nanoray.glint.getConfig
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.dependency.register
import pl.shockah.unikorn.dependency.resolve

class MessageCommandPlugin(
		container: Container
): ContainerEnabledPlugin(container) {
	private val jda: JDA by resolver.inject()
	private val commandManager: MessageCommandManager by resolver.inject()

	private val pluginContainer = Container(resolver)
	private val eventListener by lazy { DiscordEventListener(container) }

	private val helpCommand = HelpCommand(resolver)
	private val pluginsCommand = PluginCommand(resolver)

	init {
		pluginContainer.register { it.resolve<ConfigManager>().getConfig<Config>() ?: throw IllegalArgumentException("Cannot parse Config.") }
		register<MessageCommandParser> { MessageCommandParserImpl(it) }
		register<MessageCommandManager> {
			val config = pluginContainer.resolve<Config>()
			val prefixParsers: List<(Message) -> String?> = config.commandPrefixes.map { prefix ->  { message -> message.contentRaw.takeIf { it.startsWith(prefix) }?.drop(prefix.length)?.trim() } }
			val regexParsers: List<(Message) -> String?> = config.commandRegexes.map { regex -> { regex.find(it.contentRaw)?.groups?.get(1)?.value?.trim() } }
			return@register MessageCommandManagerImpl(it, prefixParsers + regexParsers)
		}

		jda.addEventListener(eventListener)
		commandManager.registerMessageCommand(helpCommand)
		commandManager.registerMessageCommand(pluginsCommand)
	}

	override fun onUnload() {
		jda.removeEventListener(eventListener)
		commandManager.unregisterMessageCommand(helpCommand)
		commandManager.unregisterMessageCommand(pluginsCommand)
		super.onUnload()
	}
}