package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import pl.nanoray.glint.command.CommandPredicate
import pl.nanoray.glint.jdaextensions.UserIdentifier
import pl.nanoray.glint.jdaextensions.retrieveUser
import pl.nanoray.glint.owner.WritableOwnerManager
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import kotlin.reflect.typeOf

internal class OwnerCommand(
		private val resolver: Resolver
): MessageCommand<Unit>(typeOf<Unit>()) {
	private val jda: JDA by resolver.inject()
	private val ownerManager: WritableOwnerManager by resolver.inject()

	override val name = "owner"
	override val description = "List bot owners."
	override val subcommands = listOf(Add(), Remove())

	override fun handleCommand(message: Message, options: Unit) {
		message.reply(
				EmbedBuilder()
						.setTitle("Bot owners")
						.appendDescription(ownerManager.owners.map { jda.retrieveUser(it).complete() }.joinToString("\n") { "${it.asMention} (${it.asTag})" })
						.build()
		).queue()
	}

	@Option.Final("user", "The user to add as a bot owner.")
	inner class Add: MessageCommand<UserIdentifier>(typeOf<UserIdentifier>()) {
		override val name = "add"
		override val description = "Add a bot owner."
		override val predicates = listOf(CommandPredicate.IsOwner(resolver))

		override fun handleCommand(message: Message, options: UserIdentifier) {
			if (ownerManager.isOwner(options)) {
				val user = jda.retrieveUser(options).complete()
				message.reply("${user.asTag} is already a bot owner.").queue()
			} else {
				ownerManager.setIsOwner(options)
				message.addReaction("\uD83D\uDC4D").queue()
			}
		}
	}

	@Option.Final("user", "The user to remove as a bot owner.")
	inner class Remove: MessageCommand<UserIdentifier>(typeOf<UserIdentifier>()) {
		override val name = "remove"
		override val description = "Remove a bot owner."
		override val predicates = listOf(CommandPredicate.IsOwner(resolver))

		override fun handleCommand(message: Message, options: UserIdentifier) {
			if (!ownerManager.isOwner(options)) {
				val user = jda.retrieveUser(options).complete()
				message.reply("${user.asTag} is not a bot owner.").queue()
			} else if (ownerManager.owners.size == 1) {
				message.reply("Cannot remove the only bot owner.").queue()
			} else {
				ownerManager.setIsOwner(options, false)
				message.addReaction("\uD83D\uDC4D").queue()
			}
		}
	}
}