package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject

internal class DiscordEventListener(
		resolver: Resolver
): ListenerAdapter() {
	private val jda: JDA by resolver.inject()
	private val messageCommandManager: MessageCommandManager by resolver.inject()

	override fun onMessageReceived(event: MessageReceivedEvent) {
		if (event.isWebhookMessage || event.author.isBot || event.author.isSystem)
			return
		messageCommandManager.handleMessageCommand(event.message)
	}
}