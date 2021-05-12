package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.entities.Message
import pl.nanoray.glint.utilities.WordBuffer
import pl.nanoray.glint.utilities.createNullabilityTypeVariants
import pl.shockah.unikorn.dependency.Resolver
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.typeOf

data class MessageCommandNameAndArgumentLine(
		val commandName: String,
		val argumentLine: String
)

interface MessageCommandParser {
	class MissingRequiredOptionException(
			val optionName: String
	): Exception("Missing required option `$optionName`.")

	class MissingOptionValueException(
			val optionName: String
	): Exception("Missing value for option `$optionName`.")

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

	private enum class StringMatchType {
		Normal, Quoted, TripleQuoted
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

	private fun parseOption(element: KAnnotatedElement, type: KType, isOptional: Boolean): MessageCommand.ParsedOption? {
		element.findAnnotation<MessageCommand.Option.Named.Flag>()?.let { annotation ->
			if (type !in createNullabilityTypeVariants<Boolean>())
				return@let
			return MessageCommand.ParsedOption.Named.Flag(
					annotation.name,
					annotation.shorthand.takeIf { it.isNotBlank() },
					annotation.description,
					isOptional
			)
		}
		element.findAnnotation<MessageCommand.Option.Named>()?.let { annotation ->
			return MessageCommand.ParsedOption.Named(
					annotation.name,
					annotation.shorthand.takeIf { it.isNotBlank() },
					annotation.description,
					isOptional
			)
		}
		element.findAnnotation<MessageCommand.Option.Positional>()?.let { annotation ->
			return MessageCommand.ParsedOption.Positional(
					annotation.name,
					annotation.description,
					isOptional
			)
		}
		element.findAnnotation<MessageCommand.Option.Positional.Final>()?.let { annotation ->
			return MessageCommand.ParsedOption.Positional.Final(
					annotation.name,
					annotation.description,
					isOptional
			)
		}
		return null
	}

	override fun <T> parseCommandHelpEntryOptions(command: MessageCommand<T>): List<CommandHelpEntry.Option> {
		if (UnitMessageCommandOptionParser.canParseMessageCommandOption(command.optionsType))
			return emptyList()
		for (optionParser in optionParsers) {
			if (!optionParser.canParseMessageCommandOption(command.optionsType))
				continue
			command::class.findAnnotation<MessageCommand.Option.Positional.Final>()?.let {
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
				parameter.takeIf { !foundAnnotation }?.findAnnotation<MessageCommand.Option.Named.Flag>()?.let {
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
				parameter.takeIf { !foundAnnotation }?.findAnnotation<MessageCommand.Option.Positional.Final>()?.let {
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
		if (command.optionsType == typeOf<Unit>()) {
			@Suppress("UNCHECKED_CAST")
			return Unit as T
		}

		val wordBuffer = WordBuffer(argumentLine)
		val argumentBuffer = run {
			val argumentWords = mutableListOf<String>()
			val argumentWhitespace = mutableListOf<String>()

			var matchType: StringMatchType? = null
			val builder = StringBuilder()
			while (wordBuffer.pointer < wordBuffer.endPointer) {
				val whitespace = wordBuffer.pointer.takeIf { it > 0 }?.let { wordBuffer.whitespace[it - 1] }
				val word = wordBuffer.peekNextWord() ?: break
				wordBuffer.pointer++
				if (builder.isEmpty()) {
					if (whitespace != null)
						argumentWhitespace.add(whitespace)
					when {
						word.startsWith("\"\"\"") -> matchType = StringMatchType.TripleQuoted
						word.startsWith("\"") -> matchType = StringMatchType.Quoted
						else -> matchType = StringMatchType.Normal
					}
				} else {
					if (whitespace != null)
						builder.append(whitespace)
				}
				builder.append(word)
				when (matchType) {
					StringMatchType.TripleQuoted -> {
						if (word.endsWith("\"\"\"")) {
							argumentWords += builder.toString().dropLast(3).drop(3)
							builder.clear()
						}
					}
					StringMatchType.Quoted -> {
						if (word.endsWith("\"") && !word.endsWith("\\\"")) {
							argumentWords += builder.toString().dropLast(1).drop(1)
							builder.clear()
						}
					}
					StringMatchType.Normal -> {
						argumentWords += builder.toString()
						builder.clear()
					}
				}
			}
			if (builder.isNotEmpty())
				argumentWords += builder.toString()
			return@run WordBuffer(argumentWords, argumentWhitespace)
		}

		parseOption(command::class, command.optionsType, command.optionsType.isMarkedNullable || command.optionsType == typeOf<Unit>())?.let { option ->
			val optionText = argumentBuffer.readWhole()
			if (optionText == null) {
				when {
					command.optionsType.isMarkedNullable -> @Suppress("UNCHECKED_CAST") return null as T
					command.optionsType == typeOf<Unit>() -> @Suppress("UNCHECKED_CAST") return Unit as T
					else -> throw MessageCommandParser.MissingRequiredOptionException(option.name)
				}
			} else {
				@Suppress("UNCHECKED_CAST")
				return parseMessageCommandOption(message, command.optionsType, optionText) as? T ?: throw MessageCommandParser.MissingRequiredOptionException(option.name)
			}
		}

		constructorLoop@ for (constructor in command.optionsKlass.constructors) {
			val parameters = mutableMapOf<KParameter, Any?>()
			val parameterOptions = constructor.parameters.associateWith { parseOption(it, it.type, it.isOptional || it.type.isMarkedNullable || it.type == typeOf<Unit>()) }.mapNotNull {
				if (it.value == null) {
					if (!it.key.isOptional) {
						if (it.key.type == typeOf<Unit>())
							parameters[it.key] = Unit
						else if (it.key.type.isMarkedNullable)
							parameters[it.key] = null
					}
					return@mapNotNull null
				} else {
					return@mapNotNull it.key to it.value!!
				}
			}.toMap()
			for (parameter in constructor.parameters) {
				if (!parameter.isOptional && !parameterOptions.containsKey(parameter))
					continue@constructorLoop
			}

			val namedParameterOptions = parameterOptions
					.mapNotNull { (parameter, option) -> (option as? MessageCommand.ParsedOption.Named)?.let { parameter to it } }
					.toMap()
			val positionalParameterOptions = parameterOptions
					.mapNotNull { (parameter, option) -> (option as? MessageCommand.ParsedOption.Positional)?.let { parameter to it } }
					.sortedWith { (_, lhs), (_, rhs) ->
						if (lhs is MessageCommand.ParsedOption.Positional.Final != rhs is MessageCommand.ParsedOption.Positional.Final)
							return@sortedWith if (lhs is MessageCommand.ParsedOption.Positional.Final) 1 else -1
						return@sortedWith 0
					}.toMap()

			val handledOptions = mutableSetOf<MessageCommand.ParsedOption>()
			var finishedHandlingNamedOptions = false

			argumentBufferLoop@ while (argumentBuffer.pointer < argumentBuffer.endPointer) {
				val word = argumentBuffer.peekNextWord()!!
				if (!finishedHandlingNamedOptions && word.startsWith("-")) {
					for ((parameter, option) in namedParameterOptions) {
						if (handledOptions.contains(option))
							continue
						if (word.equals("--${option.name}", true) || (option.shorthand != null && word == "-${option.shorthand}")) {
							argumentBuffer.pointer++
							if (option is MessageCommand.ParsedOption.Named.Flag) {
								parameters[parameter] = true
							} else {
								val optionText = argumentBuffer.readNextWord() ?: throw MessageCommandParser.MissingOptionValueException(option.name)
								for (optionParser in optionParsers) {
									if (optionParser.canParseMessageCommandOption(parameter.type)) {
										val optionValue = optionParser.parseMessageCommandOption(message, parameter.type, optionText)
										parameters[parameter] = optionValue
									}
								}
							}
							handledOptions.add(option)
							if (handledOptions.containsAll(namedParameterOptions.values))
								finishedHandlingNamedOptions = true
							continue@argumentBufferLoop
						}
					}
				}
				finishedHandlingNamedOptions = true
				positionalParameterOptionsLoop@ for ((parameter, option) in positionalParameterOptions) {
					if (handledOptions.contains(option))
						continue
					for (optionParser in optionParsers) {
						if (optionParser.canParseMessageCommandOption(parameter.type)) {
							val optionText = if (option is MessageCommand.ParsedOption.Positional.Final) argumentBuffer.readWhole()!! else word
							val optionValue = optionParser.parseMessageCommandOption(message, parameter.type, optionText)
							parameters[parameter] = optionValue
							if (option !is MessageCommand.ParsedOption.Positional.Final)
								argumentBuffer.pointer++
							handledOptions.add(option)
							continue@argumentBufferLoop
						}
					}
					if (!option.isOptional)
						throw MessageCommandParser.MissingRequiredOptionException(option.name)
				}
			}
			if (argumentBuffer.pointer != argumentBuffer.endPointer)
				throw MessageCommandParser.UnhandledInputException(argumentBuffer.readWhole()!!)
			for (option in parameterOptions.values) {
				if (!option.isOptional && option !in handledOptions)
					throw MessageCommandParser.MissingRequiredOptionException(option.name)
			}
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