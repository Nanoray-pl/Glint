package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.entities.Message
import pl.nanoray.glint.utilities.createNullabilityTypeVariants
import pl.shockah.unikorn.dependency.Resolver
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

data class MessageCommandNameAndArgumentLine(
		val commandName: String,
		val argumentLine: String
)

interface MessageCommandParser {
	fun registerOptionParser(optionParser: MessageCommandOptionParser)
	fun unregisterOptionParser(optionParser: MessageCommandOptionParser)

	fun <T: Any> parseCommandHelpEntryOptions(optionsKlass: KClass<T>): List<CommandHelpEntry.Option>
	fun parseMessageCommandNameAndArgumentLine(message: Message, argumentLine: String): MessageCommandNameAndArgumentLine
	fun <T: Any> parseMessageCommandOptions(message: Message, argumentLine: String, optionsKlass: KClass<T>): T
	fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any?
}

class MessageCommandParserImpl(
		resolver: Resolver
): MessageCommandParser {
	private val optionParsers = mutableListOf(
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
			TextChannelMessageCommandOptionParser(resolver),
			VoiceChannelMessageCommandOptionParser(resolver)
	)

	override fun registerOptionParser(optionParser: MessageCommandOptionParser) {
		optionParsers.add(optionParser)
	}

	override fun unregisterOptionParser(optionParser: MessageCommandOptionParser) {
		optionParsers.remove(optionParser)
	}

	override fun <T: Any> parseCommandHelpEntryOptions(optionsKlass: KClass<T>): List<CommandHelpEntry.Option> {
		if (optionsKlass == Unit::class)
			return emptyList()

		constructorLoop@ for (constructor in optionsKlass.constructors) {
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
		throw IllegalArgumentException("$optionsKlass cannot be used as `Options` for `Command`.")
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

	override fun <T: Any> parseMessageCommandOptions(message: Message, argumentLine: String, optionsKlass: KClass<T>): T {
		if (optionsKlass == Unit::class)
			@Suppress("UNCHECKED_CAST")
			return Unit as T

		constructorLoop@ for (constructor in optionsKlass.constructors) {
			val parameters = mutableMapOf<KParameter, Any?>()
			var argumentLineLeft = argumentLine
			parameterLoop@ for (parameter in constructor.parameters) {
				var optionName: String? = null
				var optionShorthand: String? = null
				var isFlag = false
				var shouldParseWhole = false

				var foundAnnotation = false
				if (!foundAnnotation) {
					val annotation = parameter.findAnnotation<MessageCommand.Option.Flag>()
					if (annotation != null) {
						if (parameter.type !in createNullabilityTypeVariants<Boolean>())
							continue@constructorLoop
						optionName = annotation.name
						optionShorthand = annotation.shorthand.takeIf { it.isNotBlank() }
						isFlag = true
						foundAnnotation = true
					}
				}
				if (!foundAnnotation) {
					val annotation = parameter.findAnnotation<MessageCommand.Option.Named>()
					if (annotation != null) {
						optionName = annotation.name
						optionShorthand = annotation.shorthand.takeIf { it.isNotBlank() }
						foundAnnotation = true
					}
				}
				if (!foundAnnotation) {
					val annotation = parameter.findAnnotation<MessageCommand.Option.Positional>()
					if (annotation != null) {
						optionName = annotation.name
						foundAnnotation = true
					}
				}
				if (!foundAnnotation) {
					val annotation = parameter.findAnnotation<MessageCommand.Option.Final>()
					if (annotation != null) {
						optionName = annotation.name
						shouldParseWhole = true
						foundAnnotation = true
					}
				}
				if (!foundAnnotation && !parameter.isOptional)
					continue@parameterLoop

				fun nextString(): String? {
					if (shouldParseWhole) {
						val result = argumentLineLeft
						argumentLineLeft = ""
						return result
					} else if (argumentLineLeft.isEmpty()) {
						return null
					} else {
						if (argumentLineLeft[0] == '"' && argumentLineLeft.length >= 2 && argumentLineLeft.count { it == '"' } >= 2) {
							if (argumentLineLeft[1] == '"') {
								argumentLineLeft = argumentLineLeft.substring(2).trim()
								return ""
							} else {
								for (i in 2 until argumentLineLeft.length) {
									if (argumentLineLeft[i] == '"' && argumentLineLeft[i - 1] != '\\') {
										val result = argumentLineLeft.take(i).drop(1).replace("\\\"", "\"").replace("\\\\", "\\")
										argumentLineLeft = argumentLineLeft.drop(i + 1).trim()
										return result
									}
								}
								return null
							}
						} else {
							for (i in 1 until argumentLineLeft.length) {
								if (argumentLineLeft[i].isWhitespace()) {
									val result = argumentLineLeft.take(i + 1)
									argumentLineLeft = argumentLineLeft.drop(i + 1).trim()
									return result
								}
							}
							val result = argumentLineLeft
							argumentLineLeft = ""
							return result
						}
					}
				}

				if (optionName != null || optionShorthand != null) {
					val oldArgumentLineLeft = argumentLineLeft
					val string = nextString()
					if (string == null) {
						if (!parameter.isOptional)
							continue@constructorLoop
						argumentLineLeft = oldArgumentLineLeft
					} else {
						var matched = false
						if (!matched && optionName != null && string == "--$optionName")
							matched = true
						if (!matched && optionShorthand != null && string == "-$optionShorthand")
							matched = true
						if (!matched) {
							if (!parameter.isOptional)
								continue@constructorLoop
							argumentLineLeft = oldArgumentLineLeft
						}
					}
				}

				if (isFlag) {
					parameters[parameter] = true
					continue
				}

				val textOption = nextString() ?: if (parameter.isOptional) continue@parameterLoop else continue@constructorLoop
				val parsedOption = parseMessageCommandOption(message, parameter, textOption) ?: continue@constructorLoop
				parameters[parameter] = parsedOption
			}
			try {
				return constructor.callBy(parameters)
			} catch (_: Exception) { }
		}
		throw IllegalArgumentException("$optionsKlass cannot be used as `Options` for `Command`.")
	}

	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return optionParsers.firstNotNullOfOrNull { it.parseMessageCommandOption(message, parameter, text) }
	}
}