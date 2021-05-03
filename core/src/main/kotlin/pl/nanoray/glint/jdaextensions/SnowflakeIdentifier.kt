package pl.nanoray.glint.jdaextensions

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.interactions.commands.Command

inline class SnowflakeIdentifier<out SnowflakeType: ISnowflake>(val value: Long)

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
typealias VoiceChannelIdentifier = SnowflakeIdentifier<VoiceChannel>
typealias WebhookIdentifier = SnowflakeIdentifier<Webhook>