package pl.nanoray.glint.slashcommand

import io.reactivex.rxjava3.core.Completable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import pl.nanoray.glint.jdaextensions.GuildIdentifier
import pl.nanoray.glint.jdaextensions.getGuild
import pl.nanoray.glint.jdaextensions.identifier
import pl.nanoray.glint.jdaextensions.toSingle
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import javax.annotation.CheckReturnValue

interface SlashCommandManager {
	fun registerSlashCommandProvider(commandProvider: SlashCommandProvider)
	fun unregisterSlashCommandProvider(commandProvider: SlashCommandProvider)

	fun getMatchingSlashCommand(guild: GuildIdentifier?, name: String, subcommandGroup: String?, subcommandName: String?): SlashCommand.Simple<*>?

	fun getMatchingSlashCommand(event: SlashCommandEvent): SlashCommand.Simple<*>? {
		return getMatchingSlashCommand(if (event.isFromGuild) event.guild?.identifier ?: return null else null, event.name, event.subcommandGroup, event.subcommandName)
	}

	@CheckReturnValue fun updateGlobalSlashCommands(): Completable
	@CheckReturnValue fun updateGuildSlashCommands(guild: GuildIdentifier): Completable
	@CheckReturnValue fun updateAllSlashCommands(): Completable
}

internal class SlashCommandManagerImpl(
	resolver: Resolver,
	private val globalCommandsAsGuildCommands: Boolean = false
): SlashCommandManager {
	private val jda: JDA by resolver.inject()
	private val slashCommandDataParser: SlashCommandDataParser by resolver.inject()

	private val commandProviders = mutableSetOf<SlashCommandProvider>()
	private val globalCommands = mutableSetOf<SlashCommand>()
	private val guildCommands = mutableMapOf<GuildIdentifier, MutableSet<SlashCommand>>()

	override fun registerSlashCommandProvider(commandProvider: SlashCommandProvider) {
		commandProviders.add(commandProvider)
	}

	override fun unregisterSlashCommandProvider(commandProvider: SlashCommandProvider) {
		commandProviders.remove(commandProvider)
	}

	override fun getMatchingSlashCommand(guild: GuildIdentifier?, name: String, subcommandGroup: String?, subcommandName: String?): SlashCommand.Simple<*>? {
		val commands = if (guild == null) globalCommands else guildCommands[guild] ?: return null
		for (command in commands) {
			val actualCommand = command.getCommandMatchingPath(name, subcommandGroup, subcommandName)
			if (actualCommand != null)
				return actualCommand
		}
		return null
	}

	@CheckReturnValue
	override fun updateGlobalSlashCommands(): Completable {
		if (globalCommandsAsGuildCommands)
			return jda.updateCommands()
				.toSingle()
				.ignoreElement()

		val commands = commandProviders.flatMap { it.globalSlashCommands }
		globalCommands.clear()
		globalCommands.addAll(commands)
		val commandData = commands.map { slashCommandDataParser.getSlashCommandData(it) }
		return jda.updateCommands()
			.addCommands(commandData)
			.toSingle()
			.ignoreElement()
	}

	@CheckReturnValue
	override fun updateGuildSlashCommands(guild: GuildIdentifier): Completable {
		val guildEntity = jda.getGuild(guild) ?: return Completable.complete()
		val commands = mutableSetOf<SlashCommand>()
		commandProviders.forEach {
			if (globalCommandsAsGuildCommands)
				commands += it.globalSlashCommands
			commands += it.getGuildSlashCommands(guild)
		}
		guildCommands[guild] = commands

		val commandData = commands.map { slashCommandDataParser.getSlashCommandData(it) }
		return guildEntity.updateCommands()
			.addCommands(commandData)
			.toSingle()
			.ignoreElement()
	}

	@CheckReturnValue
	override fun updateAllSlashCommands(): Completable {
		return updateGlobalSlashCommands()
			.andThen(Completable.merge(jda.guilds.map { updateGuildSlashCommands(it.identifier) }))
	}
}