package pl.nanoray.glint.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import pl.nanoray.glint.jdaextensions.GuildIdentifier
import pl.nanoray.glint.jdaextensions.getGuild
import pl.nanoray.glint.owner.OwnerManager
import pl.nanoray.glint.owner.isOwner
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject

sealed interface CommandPredicate {
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

	fun interface UserContext: CommandPredicate {
		fun isMessageCommandAllowed(user: User): Result
	}

	fun interface ChannelUserContext: CommandPredicate {
		fun isMessageCommandAllowed(channel: MessageChannel, user: User): Result
	}

	fun interface ChannelUserOptionsContext<T: Any>: CommandPredicate {
		fun isMessageCommandAllowed(channel: MessageChannel, user: User, options: T): Result
	}

	class IsOwner(
			resolver: Resolver
	): UserContext {
		private val ownerManager: OwnerManager by resolver.inject()

		override fun isMessageCommandAllowed(user: User): Result {
			val isAllowed = ownerManager.isOwner(user)
			return Result(isAllowed, "This command is only for the bot owners.")
		}
	}

	class HasGuildPermissions(
			private val permissions: Collection<Permission>
	): ChannelUserContext {
		constructor(vararg permissions: Permission): this(permissions.toList())

		override fun isMessageCommandAllowed(channel: MessageChannel, user: User): Result {
			val isAllowed = (channel as? GuildChannel)?.guild?.retrieveMember(user)?.complete()?.hasPermission(permissions) == true
			return Result(isAllowed, "This command requires additional permissions: ${permissions.joinToString(", ") { it.getName() }}.")
		}

		class FromOptions<T: Any>(
				private val permissions: Collection<Permission>,
				private val guildSupplier: (T) -> Guild
		): ChannelUserOptionsContext<T> {
			constructor(
					vararg permissions: Permission,
					guildSupplier: (T) -> Guild
			): this(permissions.toList(), guildSupplier)

			override fun isMessageCommandAllowed(channel: MessageChannel, user: User, options: T): Result {
				val guild = guildSupplier(options)
				val isAllowed = guild.retrieveMember(user).complete()?.hasPermission(permissions) == true
				return Result(isAllowed, "This command requires additional permissions: ${permissions.joinToString(", ") { it.getName() }}.")
			}

			class UsingIdentifier<T: Any>(
					resolver: Resolver,
					private val permissions: Collection<Permission>,
					private val guildSupplier: (T) -> GuildIdentifier
			): ChannelUserOptionsContext<T> {
				constructor(
						resolver: Resolver,
						vararg permissions: Permission,
						guildSupplier: (T) -> GuildIdentifier
				): this(resolver, permissions.toList(), guildSupplier)

				private val jda: JDA by resolver.inject()

				override fun isMessageCommandAllowed(channel: MessageChannel, user: User, options: T): Result {
					val guild = guildSupplier(options)
					val isAllowed = jda.getGuild(guild)?.retrieveMember(user)?.complete()?.hasPermission(permissions) == true
					return Result(isAllowed, "This command requires additional permissions: ${permissions.joinToString(", ") { it.getName() }}.")
				}
			}
		}
	}
}