package pl.nanoray.glint.command

import io.reactivex.rxjava3.core.Completable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import pl.nanoray.glint.jdaextensions.GuildIdentifier
import pl.nanoray.glint.jdaextensions.asCompletable
import pl.nanoray.glint.jdaextensions.getGuild
import pl.nanoray.glint.jdaextensions.identifier
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import javax.annotation.CheckReturnValue

interface CommandManager {
	fun registerCommandProvider(commandProvider: CommandProvider)
	fun unregisterCommandProvider(commandProvider: CommandProvider)

	fun getMatchingCommand(guild: GuildIdentifier?, name: String, subcommandGroup: String?, subcommandName: String?): Command.Simple<*>?

	fun getMatchingCommand(event: SlashCommandEvent): Command.Simple<*>? {
		return getMatchingCommand(if (event.isFromGuild) event.guild?.identifier ?: return null else null, event.name, event.subcommandGroup, event.subcommandName)
	}

	@CheckReturnValue fun updateGlobalCommands(): Completable
	@CheckReturnValue fun updateGuildCommands(guild: GuildIdentifier): Completable
	@CheckReturnValue fun updateAllCommands(): Completable
}

internal class CommandManagerImpl(
		resolver: Resolver,
		private val globalCommandsAsGuildCommands: Boolean = false
): CommandManager {
	private val jda: JDA by resolver.inject()
	private val commandDataParser: CommandDataParser by resolver.inject()

	private val commandProviders = mutableSetOf<CommandProvider>()
	private val globalCommands = mutableSetOf<Command>()
	private val guildCommands = mutableMapOf<GuildIdentifier, MutableSet<Command>>()

	override fun registerCommandProvider(commandProvider: CommandProvider) {
		commandProviders.add(commandProvider)
	}

	override fun unregisterCommandProvider(commandProvider: CommandProvider) {
		commandProviders.remove(commandProvider)
	}

	override fun getMatchingCommand(guild: GuildIdentifier?, name: String, subcommandGroup: String?, subcommandName: String?): Command.Simple<*>? {
		val commands = if (guild == null) globalCommands else guildCommands[guild] ?: return null
		for (command in commands) {
			val actualCommand = command.getCommandMatchingPath(name, subcommandGroup, subcommandName)
			if (actualCommand != null)
				return actualCommand
		}
		return null
	}

	@CheckReturnValue
	override fun updateGlobalCommands(): Completable {
		if (globalCommandsAsGuildCommands)
			return jda.updateCommands()
					.asCompletable()

		val commands = commandProviders.flatMap { it.globalCommands }
		globalCommands.clear()
		globalCommands.addAll(commands)
		val commandData = commands.map { commandDataParser.getCommandData(it) }
		return jda.updateCommands()
				.addCommands(commandData)
				.asCompletable()
	}

	@CheckReturnValue
	override fun updateGuildCommands(guild: GuildIdentifier): Completable {
		val guildEntity = jda.getGuild(guild) ?: return Completable.complete()
		val commands = mutableSetOf<Command>()
		commandProviders.forEach {
			if (globalCommandsAsGuildCommands)
				commands += it.globalCommands
			commands += it.getGuildCommands(guild)
		}
		guildCommands[guild] = commands

		val commandData = commands.map { commandDataParser.getCommandData(it) }
		return guildEntity.updateCommands()
				.addCommands(commandData)
				.asCompletable()
	}

	@CheckReturnValue
	override fun updateAllCommands(): Completable {
		return updateGlobalCommands()
				.andThen(jda.guilds.map { updateGuildCommands(it.identifier) }.let {
					when (it.size) {
						0 -> Completable.complete()
						1 -> it.single()
						else -> Completable.merge(it)
					}
				})
	}
}