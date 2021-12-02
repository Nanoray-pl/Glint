package pl.nanoray.glint.spammoderation

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import pl.nanoray.glint.ConfigManager
import pl.nanoray.glint.getConfig
import pl.nanoray.glint.jdaextensions.UserIdentifier
import pl.nanoray.glint.jdaextensions.identifier
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import java.util.concurrent.locks.ReentrantLock

internal class DiscordEventListener(
	resolver: Resolver
): ListenerAdapter() {
	private val jda: JDA by resolver.inject()
	private val configManager: ConfigManager by resolver.inject()
	private val discordModerationWorker: DiscordModerationWorker by resolver.inject()

	private val config by lazy { configManager.getConfig<Config>() ?: throw IllegalArgumentException("Cannot parse Config.") }

	private val lock = ReentrantLock()
	private val userFilters = mutableMapOf<UserIdentifier, SingleUserModerationFilter>()

	override fun onMessageReceived(event: MessageReceivedEvent) {
		if (event.isWebhookMessage || event.author.isBot || event.author.isSystem)
			return

		val userFilter = userFilters.computeIfAbsent(event.author.identifier) { SingleUserModerationFilterImpl(config, it, discordModerationWorker) }
		userFilter.addMessage(event.message)
	}
}