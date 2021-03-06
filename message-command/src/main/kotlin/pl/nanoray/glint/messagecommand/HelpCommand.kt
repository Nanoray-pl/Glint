package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import pl.nanoray.glint.command.CommandPredicate
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import kotlin.reflect.typeOf

internal class HelpCommand(
	resolver: Resolver
): MessageCommand<HelpCommand.Options>(typeOf<Options>()) {
	override val name = "help"
	override val description = "This very command."
	override val additionalDescription = """
		If `command` is provided, shows information about the specific command.
		If `command` is omitted, lists all of the available commands.
	""".trimIndent()

	data class Options(
		@Option.Positional.Final("command", "The command to show information about.") val commandName: String? = null
	)

	private val commandManager: MessageCommandManager by resolver.inject()
	private val commandParser: MessageCommandParser by resolver.inject()

	private val whitespaceRegex = Regex("\\s+")

	override fun handleCommand(message: Message, options: Options) {
		if (options.commandName == null)
			handleListCommand(message)
		else
			handleInfoCommand(message, options.commandName)
	}

	private fun EmbedBuilder.addCommandInfoFields(callee: User, commandChain: List<MessageCommand<*>>): EmbedBuilder {
		fun getFlatCommands(commandChain: List<MessageCommand<*>> = emptyList(), command: MessageCommand<*>): List<List<MessageCommand<*>>> {
			val newCommandChain = commandChain + command
			val results = mutableListOf<List<MessageCommand<*>>>()
			if (command.optionsKlass != Nothing::class)
				results += newCommandChain
			command.subcommands.forEach { results += getFlatCommands(newCommandChain, it) }
			return results
		}

		val command = commandChain.last()
		val allowedSubcommands = command.subcommands.filter {
			for (predicate in it.predicates) {
				when (predicate) {
					is CommandPredicate.UserContext -> {
						if (!predicate.isMessageCommandAllowed(callee).isAllowed)
							return@filter false
					}
					else -> continue
				}
			}
			return@filter true
		}
		if (allowedSubcommands.isNotEmpty())
			addField("Subcommands", allowedSubcommands.joinToString(", ") { "`${it.name}`" }, false)
		addField("Usage", getFlatCommands(command = command).joinToString("\n") { "`${getCommandUsageString(commandChain.dropLast(1) + it)}`" }, false)
		if (command.additionalDescription != null)
			addField("Additional info", command.additionalDescription, false)
		return this
	}

	private fun handleListCommand(message: Message) {
		val groupedCommands = commandManager.messageCommands.sortedBy { it.name.lowercase() }.groupBy { it.name.first().lowercase() }
		message.reply(
			MessageBuilder(
				EmbedBuilder().apply {
					addField(
						"Command list",
						groupedCommands.entries.joinToString("\n") { it.value.joinToString(", ") { "`${it.name}`" } },
						false
					)
					addCommandInfoFields(message.author, listOf(this@HelpCommand))
				}.build()
			).build()
		).queue()
	}

	private fun handleInfoCommand(message: Message, commandName: String) {
		fun handle(nameChainLeft: List<String>, commandChain: List<MessageCommand<*>>): Boolean {
			val command = commandChain.last()
			for (predicate in command.predicates) {
				when (predicate) {
					is CommandPredicate.UserContext -> {
						if (!predicate.isMessageCommandAllowed(message.author).isAllowed)
							return false
					}
					else -> continue
				}
			}
			if (nameChainLeft.isEmpty()) {
				message.reply(
					MessageBuilder(
						EmbedBuilder().apply {
							setTitle("Command `${commandChain.joinToString(" ") { it.name }}`")
							appendDescription(commandChain.last().description)
							addCommandInfoFields(message.author, commandChain)
						}.build()
					).build()
				).queue()
				return true
			} else {
				val nextName = nameChainLeft.first()
				for (subcommand in command.subcommands) {
					if (subcommand.name.equals(nextName, true))
						return handle(nameChainLeft.drop(1), commandChain + subcommand)
				}
				return false
			}
		}

		val nameSplit = commandName.split(whitespaceRegex)
		val firstName = nameSplit.first()
		for (command in commandManager.messageCommands) {
			if (command.name.equals(firstName, true) && handle(nameSplit.drop(1), listOf(command)))
				return
		}
		message.reply("Unknown command `${nameSplit.joinToString(" ")}`.").queue()
	}

	private fun getCommandUsageString(commandChain: List<MessageCommand<*>>): String {
		val command = commandChain.last()
		val result = StringBuilder()
		result.append(commandChain.joinToString(" ") { it.name })
		for (option in commandParser.parseCommandHelpEntryOptions(command)) {
			when (option.type) {
				CommandHelpEntry.Option.Type.Flag -> {
					val optionNames = listOfNotNull("--${option.name}", option.shorthand?.let { "-$it" })
					val fullLine = optionNames.joinToString("|")
					val wrappedFullLine = if (option.isOptional) "[$fullLine]" else fullLine
					result.append(" $wrappedFullLine")
				}
				CommandHelpEntry.Option.Type.Named -> {
					val optionNames = listOfNotNull("--${option.name}", option.shorthand?.let { "-$it" })
					val fullLine = "${optionNames.joinToString("|")} <${option.name}>"
					val wrappedFullLine = if (option.isOptional) "[$fullLine]" else fullLine
					result.append(" $wrappedFullLine")
				}
				CommandHelpEntry.Option.Type.Positional -> {
					val fullLine = "<${option.name}>"
					val wrappedFullLine = if (option.isOptional) "[$fullLine]" else fullLine
					result.append(" $wrappedFullLine")
				}
				CommandHelpEntry.Option.Type.Final -> {
					val fullLine = "<${option.name}>"
					val wrappedFullLine = if (option.isOptional) "[$fullLine]" else fullLine
					result.append(" $wrappedFullLine")
				}
			}
		}
		return result.toString()
	}
}