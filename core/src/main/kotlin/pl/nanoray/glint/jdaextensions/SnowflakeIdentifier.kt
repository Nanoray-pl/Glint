package pl.nanoray.glint.jdaextensions

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.interactions.commands.Command

@Serializable(SnowflakeIdentifierSerializer::class)
data class SnowflakeIdentifier<out SnowflakeType: ISnowflake>(
	val value: Long
)

class SnowflakeIdentifierSerializer<SnowflakeType: ISnowflake>(
	longSerializer: KSerializer<Long>
): KSerializer<SnowflakeIdentifier<SnowflakeType>> {
	override val descriptor = longSerializer.descriptor

	override fun serialize(encoder: Encoder, value: SnowflakeIdentifier<SnowflakeType>) {
		encoder.encodeLong(value.value)
	}

	override fun deserialize(decoder: Decoder): SnowflakeIdentifier<SnowflakeType> {
		return SnowflakeIdentifier(decoder.decodeLong())
	}
}

val <T: ISnowflake> T.identifier: SnowflakeIdentifier<T>
	get() = SnowflakeIdentifier(idLong)

typealias CategoryIdentifier = SnowflakeIdentifier<Category>
typealias CommandIdentifier = SnowflakeIdentifier<Command>
typealias EmoteIdentifier = SnowflakeIdentifier<Emote>
typealias GuildIdentifier = SnowflakeIdentifier<Guild>
typealias GuildChannelIdentifier = SnowflakeIdentifier<GuildChannel>
typealias PrivateChannelIdentifier = SnowflakeIdentifier<PrivateChannel>
typealias RoleIdentifier = SnowflakeIdentifier<Role>
typealias StoreChannelIdentifier = SnowflakeIdentifier<StoreChannel>
typealias TextChannelIdentifier = SnowflakeIdentifier<TextChannel>
typealias UserIdentifier = SnowflakeIdentifier<User>
typealias AudioChannelIdentifier = SnowflakeIdentifier<AudioChannel>
typealias VoiceChannelIdentifier = SnowflakeIdentifier<VoiceChannel>
typealias WebhookIdentifier = SnowflakeIdentifier<Webhook>