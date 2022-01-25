package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.nanoray.glint.store.*
import pl.nanoray.glint.store.map.map
import pl.nanoray.glint.store.map.toMapStore
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject

class MessageCommandPlugin(
	container: Container
): ContainerEnabledPlugin(container) {
	private val jda: JDA by resolver.inject()
	private val commandManager: MessageCommandManager by resolver.inject()
	private val storePlugin: StorePlugin by resolver.inject()

	private val pluginContainer = Container(resolver)
	private val eventListener by lazy { DiscordEventListener(container) }

	private val helpCommand = HelpCommand(resolver)
	private val pluginCommand = PluginCommand(resolver)
	private val ownerCommand = OwnerCommand(resolver)

	private val configStore = ConfigStore<Config>(resolver)
		.replacingNull(Config())
		.throttling(15_000)

	init {
		register<MessageCommandParser> { MessageCommandParserImpl(it) }
		register<MessageCommandManager> {
			fun parseConfigEntry(entry: Config.Entry): List<(Message) -> String?> {
				val prefixParsers: List<(Message) -> String?> = entry.commandPrefixes.map { prefix -> { message -> message.contentRaw.takeIf { it.startsWith(prefix) }?.drop(prefix.length)?.trim() } }
				val regexParsers: List<(Message) -> String?> = entry.commandRegexes.map { regex -> { regex.find(it.contentRaw)?.groups?.get(1)?.value?.trim() } }
				return prefixParsers + regexParsers
			}

			return@register MessageCommandManagerImpl(
				it,
				configStore
					.map { it.perTextChannel }
					.toMapStore()
					.map { it?.let { parseConfigEntry(it) } },
				configStore
					.map { it.perCategory }
					.toMapStore()
					.map { it?.let { parseConfigEntry(it) } },
				configStore
					.map { it.perGuild }
					.toMapStore()
					.map { it?.let { parseConfigEntry(it) } },
				configStore
					.map { parseConfigEntry(it.global) }
			)
		}

		storePlugin.registerThrottleStore(configStore)

		jda.addEventListener(eventListener)
		commandManager.registerMessageCommand(helpCommand)
		commandManager.registerMessageCommand(pluginCommand)
		commandManager.registerMessageCommand(ownerCommand)
	}

	override fun onUnload() {
		storePlugin.unregisterThrottleStore(configStore)

		jda.removeEventListener(eventListener)
		commandManager.unregisterMessageCommand(helpCommand)
		commandManager.unregisterMessageCommand(pluginCommand)
		commandManager.unregisterMessageCommand(ownerCommand)
		super.onUnload()
	}
}