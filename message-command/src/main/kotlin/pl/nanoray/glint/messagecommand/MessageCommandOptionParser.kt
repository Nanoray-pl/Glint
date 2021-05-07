package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import pl.nanoray.glint.jdaextensions.*
import pl.nanoray.glint.utilities.createNullabilityTypeVariants
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KParameter

fun interface MessageCommandOptionParser {
	fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any?
}

object BooleanMessageCommandOptionParser: MessageCommandOptionParser {
	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<Boolean>() -> when (text.lowercase()) {
				in listOf("true", "t", "yes", "y", "1") -> true
				in listOf("false", "f", "no", "n", "0") -> false
				else -> null
			}
			else -> null
		}
	}
}

object IntMessageCommandOptionParser: MessageCommandOptionParser {
	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<Int>() -> text.toIntOrNull()
			else -> null
		}
	}
}

object LongMessageCommandOptionParser: MessageCommandOptionParser {
	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<Long>() -> text.toLongOrNull()
			else -> null
		}
	}
}

object BigIntegerMessageCommandOptionParser: MessageCommandOptionParser {
	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<BigInteger>() -> text.toBigIntegerOrNull()
			else -> null
		}
	}
}

object FloatMessageCommandOptionParser: MessageCommandOptionParser {
	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<Float>() -> text.toFloatOrNull()
			else -> null
		}
	}
}

object DoubleMessageCommandOptionParser: MessageCommandOptionParser {
	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<Double>() -> text.toDoubleOrNull()
			else -> null
		}
	}
}

object BigDecimalMessageCommandOptionParser: MessageCommandOptionParser {
	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<BigDecimal>() -> text.toBigDecimalOrNull()
			else -> null
		}
	}
}

object StringMessageCommandOptionParser: MessageCommandOptionParser {
	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<String>() -> text
			else -> null
		}
	}
}

class UserMessageCommandOptionParser(
		resolver: Resolver
): MessageCommandOptionParser {
	private val jda: JDA by resolver.inject()

	private val regex = Regex("<@!?(\\d+)>")

	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<UserIdentifier>() -> parseUserIdentifier(message, text)
			in createNullabilityTypeVariants<User>() -> parseUser(message, text)
			else -> null
		}
	}

	private fun parseUserIdentifier(message: Message, text: String): UserIdentifier? {
		regex.find(text)?.groups?.get(1)?.value?.let { return UserIdentifier(it.toLong()) }
		text.toLongOrNull()?.let { jda.getUserById(it) }?.let { return it.identifier }
		(message.channel as? GuildChannel)?.guild?.let {
			it.getMembersByName(text, true).singleOrNull()?.let { return it.user.identifier }
			it.getMemberByTag(text)?.let { return it.user.identifier }
		}
		jda.getUserByTag(text)?.let { return it.identifier }
		return null
	}

	private fun parseUser(message: Message, text: String): User? {
		val id = parseUserIdentifier(message, text) ?: return null
		return jda.getUser(id)
	}
}

class RoleMessageCommandOptionParser(
		resolver: Resolver
): MessageCommandOptionParser {
	private val jda: JDA by resolver.inject()

	private val regex = Regex("<@&(\\d+)>")

	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<RoleIdentifier>() -> parseRoleIdentifier(message, text)
			in createNullabilityTypeVariants<Role>() -> parseRole(message, text)
			else -> null
		}
	}

	private fun parseRoleIdentifier(message: Message, text: String): RoleIdentifier? {
		regex.find(text)?.groups?.get(1)?.value?.let { return RoleIdentifier(it.toLong()) }
		text.toLongOrNull()?.let { jda.getRoleById(it) }?.let { return it.identifier }
		(message.channel as? GuildChannel)?.guild?.getRolesByName(text, true)?.singleOrNull()?.let { return it.identifier }
		return null
	}

	private fun parseRole(message: Message, text: String): Role? {
		val id = parseRoleIdentifier(message, text) ?: return null
		return jda.getRole(id)
	}
}

class TextChannelMessageCommandOptionParser(
		resolver: Resolver
): MessageCommandOptionParser {
	private val jda: JDA by resolver.inject()

	private val regex = Regex("<#(\\d+)>")

	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<TextChannelIdentifier>() -> parseTextChannelIdentifier(message, text)
			in createNullabilityTypeVariants<TextChannel>() -> parseTextChannel(message, text)
			else -> null
		}
	}

	private fun parseTextChannelIdentifier(message: Message, text: String): TextChannelIdentifier? {
		regex.find(text)?.groups?.get(1)?.value?.let { return TextChannelIdentifier(it.toLong()) }
		text.toLongOrNull()?.let { jda.getTextChannelById(it) }?.let { return it.identifier }
		(message.channel as? GuildChannel)?.guild?.getTextChannelsByName(text, true)?.singleOrNull()?.let { return it.identifier }
		return null
	}

	private fun parseTextChannel(message: Message, text: String): TextChannel? {
		val id = parseTextChannelIdentifier(message, text) ?: return null
		return jda.getTextChannel(id)
	}
}

class VoiceChannelMessageCommandOptionParser(
		resolver: Resolver
): MessageCommandOptionParser {
	private val jda: JDA by resolver.inject()

	override fun parseMessageCommandOption(message: Message, parameter: KParameter, text: String): Any? {
		return when (parameter.type) {
			in createNullabilityTypeVariants<VoiceChannelIdentifier>() -> parseVoiceChannelIdentifier(message, text)
			in createNullabilityTypeVariants<VoiceChannel>() -> parseVoiceChannel(message, text)
			else -> null
		}
	}

	private fun parseVoiceChannelIdentifier(message: Message, text: String): VoiceChannelIdentifier? {
		text.toLongOrNull()?.let { jda.getVoiceChannelById(it) }?.let { return it.identifier }
		(message.channel as? GuildChannel)?.guild?.getVoiceChannelsByName(text, true)?.singleOrNull()?.let { return it.identifier }
		return null
	}

	private fun parseVoiceChannel(message: Message, text: String): VoiceChannel? {
		val id = parseVoiceChannelIdentifier(message, text) ?: return null
		return jda.getVoiceChannel(id)
	}
}