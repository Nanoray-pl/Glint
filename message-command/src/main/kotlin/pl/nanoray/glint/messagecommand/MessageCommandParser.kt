package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.entities.Message
import pl.nanoray.glint.utilities.createNullabilityTypeVariants
import pl.shockah.unikorn.dependency.Resolver
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation

data class MessageCommandNameAndArgumentLine(
		val commandName: String,
		val argumentLine: String
)

interface MessageCommandParser {
	class MissingRequiredOptionException(
			val optionName: String
	): Exception("Missing required option `$optionName`.")

	class UnhandledInputException(
			val input: String
	): Exception("Unhandled command input: `$input`.")

	fun registerOptionParser(optionParser: MessageCommandOptionParser)
	fun unregisterOptionParser(optionParser: MessageCommandOptionParser)

	fun <T> parseCommandHelpEntryOptions(command: MessageCommand<T>): List<CommandHelpEntry.Option>
	fun parseMessageCommandNameAndArgumentLine(message: Message, argumentLine: String): MessageCommandNameAndArgumentLine
	fun <T> parseMessageCommandOptions(message: Message, argumentLine: String, command: MessageCommand<T>): T
	fun parseMessageCommandOption(message: Message, type: KType, text: String): Any?
}

internal class MessageCommandParserImpl(
		resolver: Resolver
): MessageCommandParser {
	private data class OptionParseResult(
			val name: String,
			val shorthand: String?,
			val isFlag: Boolean = false,
			val shouldParseWhole: Boolean = false
	)

	private sealed class OptionValue<T> {
		data class Provided<T>(
				val value: T
		): OptionValue<T>()

		data class OptionalNotProvided<T>(
				val optionName: String
		): OptionValue<T>()

		data class RequiredNotProvided<T>(
				val optionName: String
		): OptionValue<T>()

		class Failed<T>: OptionValue<T>()
	}

	private val optionParsers = mutableListOf(
			UnitMessageCommandOptionParser,
			BooleanMessageCommandOptionParser,
			IntMessageCommandOptionParser,
			LongMessageCommandOptionParser,
			BigIntegerMessageCommandOptionParser,
			FloatMessageCommandOptionParser,
			DoubleMessageCommandOptionParser,
			BigDecimalMessageCommandOptionParser,
			StringMessageCommandOptionParser,
			UserMessageCommandOptionParser(resolver),
			RoleMessageCommandOptionParser(resolver),
			GuildChannelMessageCommandOptionParser(resolver),
			TextChannelMessageCommandOptionParser(resolver),
			VoiceChannelMessageCommandOptionParser(resolver)
	)

	override fun registerOptionParser(optionParser: MessageCommandOptionParser) {
		optionParsers.add(optionParser)
	}

	override fun unregisterOptionParser(optionParser: MessageCommandOptionParser) {
		optionParsers.remove(optionParser)
	}

	override fun <T> parseCommandHelpEntryOptions(command: MessageCommand<T>): List<CommandHelpEntry.Option> {
		if (UnitMessageCommandOptionParser.canParseMessageCommandOption(command.optionsType))
			return emptyList()
		for (optionParser in optionParsers) {
			if (!optionParser.canParseMessageCommandOption(command.optionsType))
				continue
			command::class.findAnnotation<MessageCommand.Option.Flag>()?.let {
				return listOf(CommandHelpEntry.Option(
						it.name,
						it.shorthand.takeIf { it.isNotBlank() },
						it.description,
						command.optionsType.isMarkedNullable,
						CommandHelpEntry.Option.Type.Flag
				))
			}
			command::class.findAnnotation<MessageCommand.Option.Named>()?.let {
				return listOf(CommandHelpEntry.Option(
						it.name,
						it.shorthand.takeIf { it.isNotBlank() },
						it.description,
						command.optionsType.isMarkedNullable,
						CommandHelpEntry.Option.Type.Named
				))
			}
			command::class.findAnnotation<MessageCommand.Option.Positional>()?.let {
				return listOf(CommandHelpEntry.Option(
						it.name,
						null,
						it.description,
						command.optionsType.isMarkedNullable,
						CommandHelpEntry.Option.Type.Positional
				))
			}
			command::class.findAnnotation<MessageCommand.Option.Final>()?.let {
				return listOf(CommandHelpEntry.Option(
						it.name,
						null,
						it.description,
						command.optionsType.isMarkedNullable,
						CommandHelpEntry.Option.Type.Final
				))
			}
		}
		constructorLoop@ for (constructor in command.optionsKlass.constructors) {
			val options = mutableListOf<CommandHelpEntry.Option>()
			parameterLoop@ for (parameter in constructor.parameters) {
				var foundAnnotation = false
				parameter.takeIf { !foundAnnotation }?.findAnnotation<MessageCommand.Option.Flag>()?.let {
					foundAnnotation = true
					options.add(CommandHelpEntry.Option(
							it.name,
							it.shorthand.takeIf { it.isNotBlank() },
							it.description,
							parameter.isOptional,
							CommandHelpEntry.Option.Type.Flag
					))
				}
				parameter.takeIf { !foundAnnotation }?.findAnnotation<MessageCommand.Option.Named>()?.let {
					foundAnnotation = true
					options.add(CommandHelpEntry.Option(
							it.name,
							it.shorthand.takeIf { it.isNotBlank() },
							it.description,
							parameter.isOptional,
							CommandHelpEntry.Option.Type.Named
					))
				}
				parameter.takeIf { !foundAnnotation }?.findAnnotation<MessageCommand.Option.Positional>()?.let {
					foundAnnotation = true
					options.add(CommandHelpEntry.Option(
							it.name,
							null,
							it.description,
							parameter.isOptional,
							CommandHelpEntry.Option.Type.Positional
					))
				}
				parameter.takeIf { !foundAnnotation }?.findAnnotation<MessageCommand.Option.Final>()?.let {
					foundAnnotation = true
					options.add(CommandHelpEntry.Option(
							it.name,
							null,
							it.description,
							parameter.isOptional,
							CommandHelpEntry.Option.Type.Final
					))
				}
				if (!foundAnnotation && !parameter.isOptional)
					continue@parameterLoop
			}
			return options
		}
		throw IllegalArgumentException("`${command.optionsKlass}` cannot be used as `Options` for `${command::class}`.")
	}

	override fun parseMessageCommandNameAndArgumentLine(message: Message, argumentLine: String): MessageCommandNameAndArgumentLine {
		for (i in 1 until argumentLine.length) {
			if (argumentLine[i].isWhitespace()) {
				val commandName = argumentLine.take(i)
				val nonCommandNameArgumentLine = argumentLine.drop(i).trim()
				return MessageCommandNameAndArgumentLine(commandName, nonCommandNameArgumentLine)
			}
		}
		return MessageCommandNameAndArgumentLine(argumentLine, "")
	}

	override fun <T> parseMessageCommandOptions(message: Message, argumentLine: String, command: MessageCommand<T>): T {
		var argumentLineLeft = argumentLine

		fun parseOption(element: KAnnotatedElement, type: KType): OptionParseResult? {
			element.findAnnotation<MessageCommand.Option.Flag>()?.let { annotation ->
				if (type !in createNullabilityTypeVariants<Boolean>())
					return@let
				return OptionParseResult(
						annotation.name,
						annotation.shorthand.takeIf { it.isNotBlank() },
						isFlag = true
				)
			}
			element.findAnnotation<MessageCommand.Option.Named>()?.let { annotation ->
				return OptionParseResult(
						annotation.name,
						annotation.shorthand.takeIf { it.isNotBlank() }
				)
			}
			element.findAnnotation<MessageCommand.Option.Positional>()?.let { annotation ->
				return OptionParseResult(
						annotation.name,
						null
				)
			}
			element.findAnnotation<MessageCommand.Option.Final>()?.let { annotation ->
				return OptionParseResult(
						annotation.name,
						null,
						shouldParseWhole = true
				)
			}
			return null
		}

		fun parseOptionValue(element: KAnnotatedElement, type: KType, isOptional: Boolean): OptionValue<Any?> {
			if (UnitMessageCommandOptionParser.canParseMessageCommandOption(type))
				return OptionValue.Provided(Unit)
			val parseOptionResult = parseOption(element, type) ?: return if (isOptional) OptionValue.Failed() else OptionValue.Failed()

			fun nextString(): String? {
				if (argumentLineLeft.isEmpty()) {
					return null
				} else if (parseOptionResult.shouldParseWhole) {
					val result = argumentLineLeft.trim()
					argumentLineLeft = ""
					return result
				} else {
					if (argumentLineLeft[0] == '"' && argumentLineLeft.length >= 2 && argumentLineLeft.count { it == '"' } >= 2) {
						if (argumentLineLeft[1] == '"') {
							argumentLineLeft = argumentLineLeft.substring(2).trim()
							return ""
						} else {
							for (i in 2 until argumentLineLeft.length) {
								if (argumentLineLeft[i] == '"' && argumentLineLeft[i - 1] != '\\') {
									val result = argumentLineLeft.take(i).drop(1).replace("\\\"", "\"").replace("\\\\", "\\").trim()
									argumentLineLeft = argumentLineLeft.drop(i + 1).trim()
									return result
								}
							}
							return null
						}
					} else {
						for (i in 1 until argumentLineLeft.length) {
							if (argumentLineLeft[i].isWhitespace()) {
								val result = argumentLineLeft.take(i + 1).trim()
								argumentLineLeft = argumentLineLeft.drop(i + 1).trim()
								return result
							}
						}
						val result = argumentLineLeft.trim()
						argumentLineLeft = ""
						return result
					}
				}
			}

			if (!parseOptionResult.shouldParseWhole) {
				val oldArgumentLineLeft = argumentLineLeft
				val string = nextString()
				if (string == null) {
					if (!isOptional)
						throw MessageCommandParser.MissingRequiredOptionException(parseOptionResult.name)
					argumentLineLeft = oldArgumentLineLeft
				} else {
					var matched = false
					if (!matched && string == "--${parseOptionResult.name}")
						matched = true
					if (!matched && parseOptionResult.shorthand != null && string == "-${parseOptionResult.shorthand}")
						matched = true
					if (!matched) {
						if (!isOptional)
							throw MessageCommandParser.MissingRequiredOptionException(parseOptionResult.name)
						argumentLineLeft = oldArgumentLineLeft
					}
				}
			}

			if (parseOptionResult.isFlag) {
				return OptionValue.Provided(true)
			}

			val textOption = nextString() ?: return if (isOptional) OptionValue.OptionalNotProvided(parseOptionResult.name) else OptionValue.RequiredNotProvided(parseOptionResult.name)
			val parsedOption = parseMessageCommandOption(message, type, textOption) ?: return OptionValue.Failed()
			return OptionValue.Provided(parsedOption)
		}

		for (optionParser in optionParsers) {
			if (!optionParser.canParseMessageCommandOption(command.optionsType))
				continue
			when (val optionValue = parseOptionValue(command::class, command.optionsType, command.optionsType.isMarkedNullable)) {
				is OptionValue.Provided -> @Suppress("UNCHECKED_CAST") return if (argumentLineLeft.isBlank()) optionValue.value as T else throw MessageCommandParser.UnhandledInputException(argumentLineLeft)
				is OptionValue.OptionalNotProvided -> {
					if (command.optionsType.isMarkedNullable) {
						@Suppress("UNCHECKED_CAST")
						return null as T
					} else {
						throw MessageCommandParser.MissingRequiredOptionException(optionValue.optionName)
					}
				}
				is OptionValue.RequiredNotProvided -> throw MessageCommandParser.MissingRequiredOptionException(optionValue.optionName)
				is OptionValue.Failed -> continue
			}
		}

		constructorLoop@ for (constructor in command.optionsKlass.constructors) {
			val parameters = mutableMapOf<KParameter, Any?>()
			parameterLoop@ for (parameter in constructor.parameters) {
				when (val optionValue = parseOptionValue(parameter, parameter.type, parameter.isOptional)) {
					is OptionValue.Provided -> parameters[parameter] = optionValue.value
					is OptionValue.OptionalNotProvided -> { }
					is OptionValue.Failed -> continue@constructorLoop
				}
			}
			if (argumentLineLeft.isNotBlank())
				throw MessageCommandParser.UnhandledInputException(argumentLineLeft)
			try {
				@Suppress("UNCHECKED_CAST")
				return constructor.callBy(parameters) as T
			} catch (_: Exception) { }
		}
		throw IllegalArgumentException("`${command.optionsKlass}` cannot be used as `Options` for `${command::class}`.")
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any? {
		return optionParsers.firstOrNull { it.canParseMessageCommandOption(type) }?.parseMessageCommandOption(message, type, text)
	}
}