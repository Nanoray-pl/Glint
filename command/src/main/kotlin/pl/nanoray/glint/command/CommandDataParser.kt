package pl.nanoray.glint.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData
import pl.nanoray.glint.jdaextensions.*
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.typeOf

data class CommandOptionsData(
		val options: List<OptionData>
) {
	operator fun plus(option: OptionData): CommandOptionsData {
		return CommandOptionsData(options + option)
	}

	fun asCommandData(name: String, description: String): CommandData {
		var data = CommandData(name, description)
		options.forEach { data = data.addOption(it) }
		return data
	}

	fun asSubcommandData(name: String, description: String): SubcommandData {
		var data = SubcommandData(name, description)
		options.forEach { data = data.addOption(it) }
		return data
	}
}

interface CommandDataParser {
	fun <T: Any> getCommandOptionsData(optionsKlass: KClass<T>): CommandOptionsData
	fun <T: Any> parseCommandOptions(event: SlashCommandEvent, optionsKlass: KClass<T>): T
}

fun CommandDataParser.getCommandData(command: Command): CommandData {
	when (command) {
		is Command.Simple<*> -> return getCommandOptionsData(command.optionsKlass).asCommandData(command.name, command.description)
		is Command.WithSubcommands -> {
			var commandData = CommandData(command.name, command.description)
			command.subcommands.forEach { commandData = commandData.addSubcommand(getCommandOptionsData(it.optionsKlass).asSubcommandData(it.name, it.description)) }
			return commandData
		}
		is Command.WithSubcommandGroups -> {
			var commandData = CommandData(command.name, command.description)
			command.groups.forEach {
				var subcommandGroupData = SubcommandGroupData(it.name, it.description)
				it.subcommands.forEach { subcommandGroupData = subcommandGroupData.addSubcommand(getCommandOptionsData(it.optionsKlass).asSubcommandData(it.name, it.description)) }
				commandData = commandData.addSubcommandGroup(subcommandGroupData)
			}
			return commandData
		}
	}
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class CommandOption(
		val name: String = "",
		val description: String
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class CommandOptionChoice(
		val name: String = "",
		val value: String = ""
)

class CommandDataParserImpl(
		resolver: Resolver
): CommandDataParser {
	val jda: JDA by resolver.inject()

	override fun <T: Any> getCommandOptionsData(optionsKlass: KClass<T>): CommandOptionsData {
		if (optionsKlass == Unit::class)
			return CommandOptionsData(emptyList())

		l@ for (constructor in optionsKlass.constructors) {
			val options = mutableListOf<OptionData>()
			for (parameter in constructor.parameters) {
				val optionAnnotation = parameter.findAnnotation<CommandOption>() ?: continue
				val optionName = optionAnnotation.name.takeIf { it.isNotBlank() } ?: parameter.name ?: continue@l
				val optionDescription = optionAnnotation.description
				val isRequired = !parameter.type.isMarkedNullable

				when {
					parameter.type.classifier in listOf(typeOf<GuildChannelIdentifier>(), typeOf<GuildChannel>()).map { it.classifier } -> {
						options.add(OptionData(OptionType.CHANNEL, optionName, optionDescription).setRequired(isRequired))
					}
					parameter.type.classifier in listOf(typeOf<TextChannelIdentifier>(), typeOf<TextChannel>()).map { it.classifier } -> {
						options.add(OptionData(OptionType.CHANNEL, optionName, optionDescription).setRequired(isRequired))
					}
					parameter.type.classifier in listOf(typeOf<VoiceChannelIdentifier>(), typeOf<VoiceChannel>()).map { it.classifier } -> {
						options.add(OptionData(OptionType.CHANNEL, optionName, optionDescription).setRequired(isRequired))
					}
					parameter.type.classifier in listOf(typeOf<CategoryIdentifier>(), typeOf<Category>()).map { it.classifier } -> {
						options.add(OptionData(OptionType.CHANNEL, optionName, optionDescription).setRequired(isRequired))
					}
					parameter.type.classifier in listOf(typeOf<UserIdentifier>(), typeOf<User>(), typeOf<Member>()).map { it.classifier } -> {
						options.add(OptionData(OptionType.USER, optionName, optionDescription).setRequired(isRequired))
					}
					parameter.type.classifier in listOf(typeOf<BigInteger>(), typeOf<Long>(), typeOf<Int>()).map { it.classifier } -> {
						options.add(OptionData(OptionType.INTEGER, optionName, optionDescription).setRequired(isRequired))
					}
					parameter.type.classifier == typeOf<Boolean>().classifier -> {
						options.add(OptionData(OptionType.BOOLEAN, optionName, optionDescription).setRequired(isRequired))
					}
					parameter.type.classifier == typeOf<String>().classifier -> {
						options.add(OptionData(OptionType.STRING, optionName, optionDescription).setRequired(isRequired))
					}
					parameter.type.javaClass.isEnum -> {
						val choices = mutableListOf<Pair<String, String>>()
						for (enumValue in parameter.type.javaClass.enumConstants) {
							val choiceAnnotation = enumValue.findAnnotation<CommandOptionChoice>() ?: continue
							val choiceName = choiceAnnotation.name.takeIf { it.isNotBlank() } ?: (enumValue as? Enum<*>)?.name ?: continue
							val choiceValue = choiceAnnotation.value.takeIf { it.isNotBlank() } ?: (enumValue as? Enum<*>)?.name ?: continue
							choices.add(choiceName to choiceValue)
						}
						if (choices.isEmpty())
							continue@l

						var optionData = OptionData(OptionType.STRING, optionName, optionDescription).setRequired(isRequired)
						for ((choiceName, choiceValue) in choices) {
							optionData = optionData.addChoice(choiceName, choiceValue)
						}
						options.add(optionData)
					}
				}
			}
			return CommandOptionsData(options)
		}
		throw IllegalArgumentException("$optionsKlass cannot be used as `Options` for `Command`.")
	}

	override fun <T: Any> parseCommandOptions(event: SlashCommandEvent, optionsKlass: KClass<T>): T {
		if (optionsKlass == Unit::class)
			@Suppress("UNCHECKED_CAST")
			return Unit as T

		constructorLoop@ for (constructor in optionsKlass.constructors) {
			val parameters = mutableMapOf<KParameter, Any?>()
			parameterLoop@ for (parameter in constructor.parameters) {
				val optionAnnotation = parameter.findAnnotation<CommandOption>() ?: continue
				val optionName = optionAnnotation.name.takeIf { it.isNotBlank() } ?: parameter.name ?: continue@constructorLoop

				val optionData = event.getOption(optionName)
				if (optionData == null) {
					if (parameter.isOptional) continue else continue@constructorLoop
				} else when {
					parameter.type in createNullabilityTypeVariants<GuildChannelIdentifier>() -> {
						parameters[parameter] = optionData.asGuildChannel.identifier
					}
					parameter.type.classifier == GuildChannel::class -> {
						parameters[parameter] = optionData.asGuildChannel
					}
					parameter.type in createNullabilityTypeVariants<TextChannelIdentifier>() -> {
						val channel = optionData.asGuildChannel as? TextChannel ?: continue@constructorLoop
						parameters[parameter] = channel.identifier
					}
					parameter.type.classifier == TextChannel::class -> {
						val channel = optionData.asGuildChannel as? TextChannel ?: continue@constructorLoop
						parameters[parameter] = channel
					}
					parameter.type in createNullabilityTypeVariants<VoiceChannelIdentifier>() -> {
						val channel = optionData.asGuildChannel as? VoiceChannel ?: continue@constructorLoop
						parameters[parameter] = channel.identifier
					}
					parameter.type.classifier == VoiceChannel::class -> {
						val channel = optionData.asGuildChannel as? VoiceChannel ?: continue@constructorLoop
						parameters[parameter] = channel
					}
					parameter.type in createNullabilityTypeVariants<CategoryIdentifier>() -> {
						val channel = optionData.asGuildChannel as? Category ?: continue@constructorLoop
						parameters[parameter] = channel.identifier
					}
					parameter.type.classifier == Category::class -> {
						val channel = optionData.asGuildChannel as? Category ?: continue@constructorLoop
						parameters[parameter] = channel
					}
					parameter.type in createNullabilityTypeVariants<UserIdentifier>() -> {
						parameters[parameter] = optionData.asUser.identifier
					}
					parameter.type.classifier == User::class -> {
						parameters[parameter] = optionData.asUser
					}
					parameter.type.classifier == Member::class -> {
						parameters[parameter] = optionData.asMember?.identifier ?: continue@constructorLoop
					}
					parameter.type.classifier == BigInteger::class -> {
						parameters[parameter] = optionData.asLong.toBigInteger()
					}
					parameter.type.classifier == Long::class -> {
						parameters[parameter] = optionData.asLong
					}
					parameter.type.classifier == Int::class -> {
						parameters[parameter] = optionData.asLong.toInt()
					}
					parameter.type.classifier == Boolean::class -> {
						parameters[parameter] = optionData.asBoolean
					}
					parameter.type.classifier == String::class -> {
						parameters[parameter] = optionData.asString
					}
					parameter.type.javaClass.isEnum -> {
						val chosenValue = optionData.asString
						for (enumValue in parameter.type.javaClass.enumConstants) {
							val choiceAnnotation = enumValue.findAnnotation<CommandOptionChoice>() ?: continue
							val choiceValue = choiceAnnotation.value.takeIf { it.isNotBlank() } ?: (enumValue as? Enum<*>)?.name ?: continue
							if (choiceValue == chosenValue) {
								parameters[parameter] = enumValue
								continue@parameterLoop
							}
						}
						continue@constructorLoop
					}
				}
			}
			return constructor.callBy(parameters)
		}
		throw IllegalArgumentException("$optionsKlass cannot be used as `Options` for `Command`.")
	}

	private inline fun <reified T: Any> createNullabilityTypeVariants(): Set<KType> {
		val source = typeOf<T>()
		return source.classifier?.let { setOf(
				it.createType(source.arguments, false, source.annotations),
				it.createType(source.arguments, true, source.annotations)
		) } ?: emptySet()
	}
}