package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.entities.Message
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface MessageCommandManager {
	val messageCommands: List<MessageCommand<*>>

	fun registerMessageCommand(command: MessageCommand<*>)
	fun unregisterMessageCommand(command: MessageCommand<*>)

	/**
	 * @return whether the command was handled.
	 */
	fun handleMessageCommand(message: Message): Boolean
}

class MessageCommandManagerImpl(
		resolver: Resolver,
		private val argumentLineParsers: List<(Message) -> String?>
): MessageCommandManager {
	companion object {
		operator fun invoke(resolver: Resolver, prefixes: List<String>): MessageCommandManagerImpl {
			return MessageCommandManagerImpl(
					resolver,
					prefixes.map { prefix ->  { message -> message.contentRaw.takeIf { it.startsWith(prefix) }?.drop(prefix.length)?.trim() } }
			)
		}
	}

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
			for (argumentLineParser in argumentLineParsers) {
				val argumentLine = argumentLineParser(message) ?: continue
				val commandNameAndArgumentLine = commandParser.parseMessageCommandNameAndArgumentLine(message, argumentLine)
				for (command in messageCommandStorage) {
					if (!command.name.equals(commandNameAndArgumentLine.commandName, true))
						continue
					val options = commandParser.parseMessageCommandOptions(message, commandNameAndArgumentLine.argumentLine, command.optionsKlass)

					@Suppress("UNCHECKED_CAST")
					val anyTypedCommand = command as? MessageCommand<Any> ?: continue
					anyTypedCommand.handleCommand(message, options)
					return@withLock true
				}
			}
		}
		return false
	}
}