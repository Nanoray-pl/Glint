package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import pl.nanoray.glint.command.CommandPredicate
import pl.nanoray.glint.jdaextensions.CategoryIdentifier
import pl.nanoray.glint.jdaextensions.GuildIdentifier
import pl.nanoray.glint.jdaextensions.TextChannelIdentifier
import pl.nanoray.glint.jdaextensions.identifier
import pl.nanoray.glint.store.Store
import pl.nanoray.glint.store.map.MapStore
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface MessageCommandManager {
	class DeniedCommandException(
		val reason: String
	): Exception(reason)

	val messageCommands: List<MessageCommand<*>>

	fun registerMessageCommand(command: MessageCommand<*>)
	fun unregisterMessageCommand(command: MessageCommand<*>)

	/**
	 * @return whether the command was handled.
	 */
	fun handleMessageCommand(message: Message): Boolean
}

internal class MessageCommandManagerImpl(
	resolver: Resolver,
	private val perTextChannelArgumentLineParsers: MapStore<TextChannelIdentifier, List<(Message) -> String?>?>,
	private val perCategoryArgumentLineParsers: MapStore<CategoryIdentifier, List<(Message) -> String?>?>,
	private val perGuildArgumentLineParsers: MapStore<GuildIdentifier, List<(Message) -> String?>?>,
	private val globalArgumentLineParsers: Store<List<(Message) -> String?>>
): MessageCommandManager {
	private val commandParser: MessageCommandParser by resolver.inject()

	private val lock = ReentrantLock()
	private val messageCommandStorage = mutableListOf<MessageCommand<*>>()

	override val messageCommands: List<MessageCommand<*>>
		get() = lock.withLock { messageCommandStorage.toList() }

	override fun registerMessageCommand(command: MessageCommand<*>) {
		lock.withLock { messageCommandStorage.add(command) }
	}

	override fun unregisterMessageCommand(command: MessageCommand<*>) {
		lock.withLock { messageCommandStorage.remove(command) }
	}

	override fun handleMessageCommand(message: Message): Boolean {
		lock.withLock {
			val textChannelGetter: () -> List<(Message) -> String?>? = { (message.channel as? TextChannel)?.identifier?.let { perTextChannelArgumentLineParsers[it] } }
			val categoryGetter: () -> List<(Message) -> String?>? = { message.category?.identifier?.let { perCategoryArgumentLineParsers[it] } }
			val guildGetter: () -> List<(Message) -> String?>? = { (message.channel as? TextChannel)?.guild?.identifier?.let { perGuildArgumentLineParsers[it] } }
			val argumentLineParsers = textChannelGetter() ?: categoryGetter() ?: guildGetter() ?: globalArgumentLineParsers.value

			for (argumentLineParser in argumentLineParsers) {
				fun handleCommand(command: MessageCommand<*>, rawArgumentLine: String): Boolean {
					val commandNameAndArgumentLine = commandParser.parseMessageCommandNameAndArgumentLine(message, rawArgumentLine)
					if (!command.name.equals(commandNameAndArgumentLine.commandName, true))
						return false

					for (predicate in command.predicates) {
						if (predicate is CommandPredicate.UserContext) {
							when (val result = predicate.isMessageCommandAllowed(message.author)) {
								CommandPredicate.Result.Allowed -> continue
								is CommandPredicate.Result.Denied -> throw MessageCommandManager.DeniedCommandException(result.reason)
							}
						}
						if (predicate is CommandPredicate.ChannelUserContext) {
							when (val result = predicate.isMessageCommandAllowed(message.channel, message.author)) {
								CommandPredicate.Result.Allowed -> continue
								is CommandPredicate.Result.Denied -> throw MessageCommandManager.DeniedCommandException(result.reason)
							}
						}
					}

					for (subcommand in command.subcommands) {
						if (handleCommand(subcommand, commandNameAndArgumentLine.argumentLine))
							return true
					}

					if (!command.name.equals(commandNameAndArgumentLine.commandName, true))
						return false
					@Suppress("UNCHECKED_CAST")
					val anyTypedCommand = command as? MessageCommand<Any> ?: return false
					val options = commandParser.parseMessageCommandOptions(message, commandNameAndArgumentLine.argumentLine, command)

					for (predicate in command.predicates) {
						if (predicate is CommandPredicate.ChannelUserOptionsContext<*>) {
							@Suppress("UNCHECKED_CAST")
							val anyTypedPredicate = command as? CommandPredicate.ChannelUserOptionsContext<Any> ?: return false
							when (val result = anyTypedPredicate.isMessageCommandAllowed(message.channel, message.author, options)) {
								CommandPredicate.Result.Allowed -> continue
								is CommandPredicate.Result.Denied -> throw MessageCommandManager.DeniedCommandException(result.reason)
							}
						}
					}

					anyTypedCommand.handleCommand(message, options)
					return true
				}

				val rawArgumentLine = argumentLineParser(message) ?: continue
				for (command in messageCommandStorage) {
					if (handleCommand(command, rawArgumentLine))
						return@withLock true
				}
			}
		}
		return false
	}
}