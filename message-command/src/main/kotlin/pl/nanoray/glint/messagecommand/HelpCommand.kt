package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject

class HelpCommand(
		resolver: Resolver
): MessageCommand<HelpCommand.Options>(Options::class) {
	override val name = "help"
	override val description = "This very command."
	override val additionalDescription = """
		If `command` is provided, shows information about the specific command.
		If `command` is omitted, lists all of the available commands.
	""".trimIndent()

	data class Options(
			@Option.Positional("command", "The command to show information about.") val commandName: String? = null
	)

	private val commandManager: MessageCommandManager by resolver.inject()
	private val commandParser: MessageCommandParser by resolver.inject()

	override fun handleCommand(message: Message, options: Options) {
		if (options.commandName == null)
			handleListCommand(message)
		else
			handleInfoCommand(message, options.commandName)
	}

	private fun handleListCommand(message: Message) {
		val commands = commandManager.messageCommands
		val groupedCommands = commands.groupBy { it.name.first().lowercase() }

		message.reply(
				EmbedBuilder().apply {
					addField(
							"Command list",
							groupedCommands.entries.joinToString("\n") { it.value.joinToString(", ") { "`${it.name}`" } },
							false
					)
					addField("Usage", "`${getCommandUsageString(this@HelpCommand)}`", false)
					addField("Additional info", additionalDescription, false)
				}.build()
		).queue()
	}

	private fun handleInfoCommand(message: Message, commandName: String) {
		val command = commandManager.messageCommands.firstOrNull { it.name.equals(commandName, true) }
		if (command == null) {
			message.reply("Unknown command `$commandName`.").queue()
			return
		}
		message.reply(
				EmbedBuilder().apply {
					setTitle("Command `${command.name}`")
					addField("Usage", "`${getCommandUsageString(command)}`", false)
					if (command.additionalDescription != null)
						addField("Additional info", additionalDescription, false)
				}.build()
		).queue()
	}

	private fun getCommandUsageString(command: MessageCommand<*>): String {
		val result = StringBuilder()
		result.append(command.name)
		for (option in commandParser.parseCommandHelpEntryOptions(command.optionsKlass)) {
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
					val fullLine = option.name
					val wrappedFullLine = if (option.isOptional) "[$fullLine]" else fullLine
					result.append(" $wrappedFullLine")
				}
			}
		}
		return result.toString()
	}
}