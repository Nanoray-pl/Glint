package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import pl.nanoray.glint.jdaextensions.*
import pl.nanoray.glint.utilities.createNullabilityTypeVariants
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KType

interface MessageCommandOptionParser {
	class ParseException(
			val reason: String
	): Exception()

	fun canParseMessageCommandOption(type: KType): Boolean

	/**
	 * @throws ParseException if there was a problem parsing `text`.
	 */
	fun parseMessageCommandOption(message: Message, type: KType, text: String): Any?
}

object UnitMessageCommandOptionParser: MessageCommandOptionParser {
	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<Unit>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return Unit
	}
}

object BooleanMessageCommandOptionParser: MessageCommandOptionParser {
	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<Boolean>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return when (text.lowercase()) {
			in listOf("true", "t", "yes", "y", "1") -> true
			in listOf("false", "f", "no", "n", "0") -> false
			else -> throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `Boolean`.")
		}
	}
}

object IntMessageCommandOptionParser: MessageCommandOptionParser {
	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<Int>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return text.toIntOrNull() ?: throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `Int`.")
	}
}

object LongMessageCommandOptionParser: MessageCommandOptionParser {
	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<Long>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return text.toLongOrNull() ?: throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `Long`.")
	}
}

object BigIntegerMessageCommandOptionParser: MessageCommandOptionParser {
	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<BigInteger>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return text.toBigIntegerOrNull() ?: throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `BigInteger`.")
	}
}

object FloatMessageCommandOptionParser: MessageCommandOptionParser {
	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<Float>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return text.toFloatOrNull() ?: throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `Float`.")
	}
}

object DoubleMessageCommandOptionParser: MessageCommandOptionParser {
	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<Double>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return text.toDoubleOrNull() ?: throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `Double`.")
	}
}

object BigDecimalMessageCommandOptionParser: MessageCommandOptionParser {
	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<BigDecimal>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return text.toBigDecimalOrNull() ?: throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `BigDecimal`.")
	}
}

object StringMessageCommandOptionParser: MessageCommandOptionParser {
	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<String>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return text
	}
}

class UserMessageCommandOptionParser(
		resolver: Resolver
): MessageCommandOptionParser {
	private val jda: JDA by resolver.inject()

	private val regex = Regex("<@!?(\\d+)>")

	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<UserIdentifier>() || type in createNullabilityTypeVariants<User>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return when (type) {
			in createNullabilityTypeVariants<UserIdentifier>() -> parseUserIdentifier(message, text)
			in createNullabilityTypeVariants<User>() -> parseUser(message, text)
			else -> throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `User`.")
		}
	}

	private fun parseUserIdentifier(message: Message, text: String): UserIdentifier {
		regex.find(text)?.let { return it.groups[1]?.value?.let { UserIdentifier(it.toLong()) } ?: throw MessageCommandOptionParser.ParseException("Cannot find user $text.") }
		text.toLongOrNull()?.let { jda.getUserById(it) }?.let { return it.identifier }
		(message.channel as? GuildChannel)?.guild?.let {
			it.getMembersByName(text, true).singleOrNull()?.let { return it.user.identifier }
			try { it.getMemberByTag(text)?.let { return it.user.identifier } } catch (_: Exception) { }
		}
		try { jda.getUserByTag(text)?.let { return it.identifier } } catch (_: Exception) { }
		throw MessageCommandOptionParser.ParseException("Cannot find user `$text`.")
	}

	private fun parseUser(message: Message, text: String): User {
		val id = parseUserIdentifier(message, text)
		return jda.getUser(id) ?: throw MessageCommandOptionParser.ParseException("Cannot find user `$text`.")
	}
}

class RoleMessageCommandOptionParser(
		resolver: Resolver
): MessageCommandOptionParser {
	private val jda: JDA by resolver.inject()

	private val regex = Regex("<@&(\\d+)>")

	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<RoleIdentifier>() || type in createNullabilityTypeVariants<Role>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return when (type) {
			in createNullabilityTypeVariants<RoleIdentifier>() -> parseRoleIdentifier(message, text)
			in createNullabilityTypeVariants<Role>() -> parseRole(message, text)
			else -> throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `Role`.")
		}
	}

	private fun parseRoleIdentifier(message: Message, text: String): RoleIdentifier {
		regex.find(text)?.let { return it.groups[1]?.value?.let { RoleIdentifier(it.toLong()) } ?: throw MessageCommandOptionParser.ParseException("Cannot find role $text.") }
		text.toLongOrNull()?.let { jda.getRoleById(it) }?.let { return it.identifier }
		(message.channel as? GuildChannel)?.guild?.getRolesByName(text, true)?.singleOrNull()?.let { return it.identifier }
		throw MessageCommandOptionParser.ParseException("Cannot find role `$text`.")
	}

	private fun parseRole(message: Message, text: String): Role {
		val id = parseRoleIdentifier(message, text)
		return jda.getRole(id) ?: throw MessageCommandOptionParser.ParseException("Cannot find role `$text`.")
	}
}

class GuildChannelMessageCommandOptionParser(
		resolver: Resolver
): MessageCommandOptionParser {
	private val jda: JDA by resolver.inject()

	private val regex = Regex("<#(\\d+)>")

	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<GuildChannelIdentifier>() || type in createNullabilityTypeVariants<GuildChannel>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return when (type) {
			in createNullabilityTypeVariants<GuildChannelIdentifier>() -> parseGuildChannelIdentifier(message, text)
			in createNullabilityTypeVariants<GuildChannel>() -> parseGuildChannel(message, text)
			else -> throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `GuildChannel`.")
		}
	}

	private fun parseGuildChannelIdentifier(message: Message, text: String): GuildChannelIdentifier {
		regex.find(text)?.let { return it.groups[1]?.value?.let { GuildChannelIdentifier(it.toLong()) } ?: throw MessageCommandOptionParser.ParseException("Cannot find channel $text.") }
		text.toLongOrNull()?.let { jda.getGuildChannelById(it) }?.let { return it.identifier }
		(message.channel as? GuildChannel)?.guild?.getTextChannelsByName(text, true)?.singleOrNull()?.let { return it.identifier }
		(message.channel as? GuildChannel)?.guild?.getVoiceChannelsByName(text, true)?.singleOrNull()?.let { return it.identifier }
		throw MessageCommandOptionParser.ParseException("Cannot find channel `$text`.")
	}

	private fun parseGuildChannel(message: Message, text: String): GuildChannel {
		val id = parseGuildChannelIdentifier(message, text)
		return jda.getGuildChannel(id) ?: throw MessageCommandOptionParser.ParseException("Cannot find channel `$text`.")
	}
}

class TextChannelMessageCommandOptionParser(
		resolver: Resolver
): MessageCommandOptionParser {
	private val jda: JDA by resolver.inject()

	private val regex = Regex("<#(\\d+)>")

	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<TextChannelIdentifier>() || type in createNullabilityTypeVariants<TextChannel>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return when (type) {
			in createNullabilityTypeVariants<TextChannelIdentifier>() -> parseTextChannelIdentifier(message, text)
			in createNullabilityTypeVariants<TextChannel>() -> parseTextChannel(message, text)
			else -> throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `TextChannel`.")
		}
	}

	private fun parseTextChannelIdentifier(message: Message, text: String): TextChannelIdentifier {
		regex.find(text)?.let { return it.groups[1]?.value?.let { TextChannelIdentifier(it.toLong()) }?.takeIf { jda.getTextChannel(it) != null } ?: throw MessageCommandOptionParser.ParseException("Cannot find text channel $text.") }
		text.toLongOrNull()?.let { jda.getTextChannelById(it) }?.let { return it.identifier }
		(message.channel as? GuildChannel)?.guild?.getTextChannelsByName(text, true)?.singleOrNull()?.let { return it.identifier }
		throw MessageCommandOptionParser.ParseException("Cannot find text channel `$text`.")
	}

	private fun parseTextChannel(message: Message, text: String): TextChannel {
		val id = parseTextChannelIdentifier(message, text)
		return jda.getTextChannel(id) ?: throw MessageCommandOptionParser.ParseException("Cannot find text channel `$text`.")
	}
}

class VoiceChannelMessageCommandOptionParser(
		resolver: Resolver
): MessageCommandOptionParser {
	private val jda: JDA by resolver.inject()

	private val regex = Regex("<#(\\d+)>")

	override fun canParseMessageCommandOption(type: KType): Boolean {
		return type in createNullabilityTypeVariants<VoiceChannelIdentifier>() || type in createNullabilityTypeVariants<VoiceChannel>()
	}

	override fun parseMessageCommandOption(message: Message, type: KType, text: String): Any {
		return when (type) {
			in createNullabilityTypeVariants<VoiceChannelIdentifier>() -> parseVoiceChannelIdentifier(message, text)
			in createNullabilityTypeVariants<VoiceChannel>() -> parseVoiceChannel(message, text)
			else -> throw MessageCommandOptionParser.ParseException("Cannot parse `$text` as `VoiceChannel`.")
		}
	}

	private fun parseVoiceChannelIdentifier(message: Message, text: String): VoiceChannelIdentifier {
		regex.find(text)?.let { return it.groups[1]?.value?.let { VoiceChannelIdentifier(it.toLong()) }?.takeIf { jda.getVoiceChannel(it) != null } ?: throw MessageCommandOptionParser.ParseException("Cannot find voice channel $text.") }
		text.toLongOrNull()?.let { jda.getVoiceChannelById(it) }?.let { return it.identifier }
		(message.channel as? GuildChannel)?.guild?.getVoiceChannelsByName(text, true)?.singleOrNull()?.let { return it.identifier }
		throw MessageCommandOptionParser.ParseException("Cannot find voice channel `$text`.")
	}

	private fun parseVoiceChannel(message: Message, text: String): VoiceChannel {
		val id = parseVoiceChannelIdentifier(message, text)
		return jda.getVoiceChannel(id) ?: throw MessageCommandOptionParser.ParseException("Cannot find voice channel `$text`.")
	}
}