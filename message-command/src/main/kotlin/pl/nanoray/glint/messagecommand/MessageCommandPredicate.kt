package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import pl.nanoray.glint.CoreConfig
import pl.nanoray.glint.jdaextensions.GuildIdentifier
import pl.nanoray.glint.jdaextensions.getGuild
import pl.nanoray.glint.jdaextensions.identifier
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject

sealed interface MessageCommandPredicate {
	sealed class Result {
		companion object {
			operator fun invoke(isAllowed: Boolean, elseReason: String): Result {
				return if (isAllowed) Allowed else Denied(elseReason)
			}
		}

		val isAllowed: Boolean
			get() = this == Allowed

		object Allowed: Result()

		data class Denied(
				val reason: String
		): Result()
	}

	fun interface UserContext: MessageCommandPredicate {
		fun isMessageCommandAllowed(user: User): Result

		class IsOwner(
				resolver: Resolver
		): UserContext {
			private val config: CoreConfig by resolver.inject()

			override fun isMessageCommandAllowed(user: User): Result {
				val isAllowed = user.identifier == config.owner
				return Result(isAllowed, "This command is only for the bot owners.")
			}
		}
	}

	fun interface MessageContext: MessageCommandPredicate {
		fun isMessageCommandAllowed(message: Message): Result

		class HasGuildPermissions(
				private val permissions: Collection<Permission>
		): MessageContext {
			constructor(vararg permissions: Permission): this(permissions.toList())

			override fun isMessageCommandAllowed(message: Message): Result {
				val isAllowed = (message.channel as? GuildChannel)?.guild?.retrieveMember(message.author)?.complete()?.hasPermission(permissions) == true
				return Result(isAllowed, "This command requires additional permissions: ${permissions.joinToString(", ") { it.getName() }}.")
			}
		}
	}

	fun interface MessageAndOptionsContext<T: Any>: MessageCommandPredicate {
		fun isMessageCommandAllowed(message: Message, options: T): Result

		class HasGuildPermissions<T: Any>(
				private val permissions: Collection<Permission>,
				private val guildSupplier: (T) -> Guild
		): MessageAndOptionsContext<T> {
			constructor(
					vararg permissions: Permission,
					guildSupplier: (T) -> Guild
			): this(permissions.toList(), guildSupplier)

			override fun isMessageCommandAllowed(message: Message, options: T): Result {
				val guild = guildSupplier(options)
				val isAllowed = guild.retrieveMember(message.author).complete()?.hasPermission(permissions) == true
				return Result(isAllowed, "This command requires additional permissions: ${permissions.joinToString(", ") { it.getName() }}.")
			}

			class UsingIdentifier<T: Any>(
					resolver: Resolver,
					private val permissions: Collection<Permission>,
					private val guildSupplier: (T) -> GuildIdentifier
			): MessageAndOptionsContext<T> {
				constructor(
						resolver: Resolver,
						vararg permissions: Permission,
						guildSupplier: (T) -> GuildIdentifier
				): this(resolver, permissions.toList(), guildSupplier)

				private val jda: JDA by resolver.inject()

				override fun isMessageCommandAllowed(message: Message, options: T): Result {
					val guild = guildSupplier(options)
					val isAllowed = jda.getGuild(guild)?.retrieveMember(message.author)?.complete()?.hasPermission(permissions) == true
					return Result(isAllowed, "This command requires additional permissions: ${permissions.joinToString(", ") { it.getName() }}.")
				}
			}
		}
	}
}