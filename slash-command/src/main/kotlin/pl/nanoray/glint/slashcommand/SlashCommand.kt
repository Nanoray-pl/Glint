package pl.nanoray.glint.slashcommand

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import pl.nanoray.glint.command.CommandPredicate
import kotlin.reflect.KClass

sealed class SlashCommand {
	abstract val name: String
	abstract val description: String
	open val predicates: List<CommandPredicate> = emptyList()

	abstract fun getCommandMatchingPath(name: String, subcommandGroup: String?, subcommandName: String?): Simple<*>?

	fun getCommandMatchingPath(event: SlashCommandEvent): Simple<*>? {
		return getCommandMatchingPath(event.name, event.subcommandGroup, event.subcommandName)
	}

	abstract class Simple<Options: Any>(
			val optionsKlass: KClass<Options>
	): SlashCommand() {
		override fun getCommandMatchingPath(name: String, subcommandGroup: String?, subcommandName: String?): Simple<*>? {
			return this.takeIf { name.equals(it.name, true) && subcommandGroup == null && subcommandName == null }
		}

		abstract fun handleCommand(event: SlashCommandEvent, options: Options)
	}

	abstract class WithSubcommands: SlashCommand() {
		abstract val subcommands: List<Simple<*>>

		override fun getCommandMatchingPath(name: String, subcommandGroup: String?, subcommandName: String?): Simple<*>? {
			if (!name.equals(this.name, true))
				return null
			return subcommands.firstOrNull { subcommandName.equals(it.name, true) }
		}
	}

	abstract class WithSubcommandGroups: SlashCommand() {
		abstract val groups: List<WithSubcommands>

		override fun getCommandMatchingPath(name: String, subcommandGroup: String?, subcommandName: String?): Simple<*>? {
			if (!name.equals(this.name, true))
				return null
			val group = groups.firstOrNull { subcommandGroup.equals(it.name, true) } ?: return null
			return group.subcommands.firstOrNull { subcommandName.equals(it.name, true) }
		}
	}

	@Retention(AnnotationRetention.RUNTIME)
	@Target(AnnotationTarget.VALUE_PARAMETER)
	annotation class Option(
			val name: String = "",
			val description: String
	) {
		@Retention(AnnotationRetention.RUNTIME)
		@Target(AnnotationTarget.PROPERTY)
		annotation class Choice(
				val name: String = "",
				val value: String = ""
		)
	}
}