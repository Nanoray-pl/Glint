package pl.nanoray.glint.logic

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import kotlin.reflect.KClass

sealed class Command {
	abstract val name: String
	abstract val description: String

	abstract fun getCommandMatchingPath(name: String, subcommandGroup: String?, subcommandName: String?): Simple<*>?

	abstract class Simple<Options: Any>(
			val optionsKlass: KClass<Options>
	): Command() {
		override fun getCommandMatchingPath(name: String, subcommandGroup: String?, subcommandName: String?): Simple<*>? {
			return this.takeIf { name.equals(it.name, true) && subcommandGroup == null && subcommandName == null }
		}

		abstract fun handleCommand(event: SlashCommandEvent, options: Options)
	}

	abstract class WithSubcommands: Command() {
		abstract val subcommands: List<Simple<*>>

		override fun getCommandMatchingPath(name: String, subcommandGroup: String?, subcommandName: String?): Simple<*>? {
			if (!name.equals(this.name, true))
				return null
			return subcommands.firstOrNull { subcommandName.equals(it.name, true) }
		}
	}

	abstract class WithSubcommandGroups: Command() {
		abstract val groups: List<WithSubcommands>

		override fun getCommandMatchingPath(name: String, subcommandGroup: String?, subcommandName: String?): Simple<*>? {
			if (!name.equals(this.name, true))
				return null
			val group = groups.firstOrNull { subcommandGroup.equals(it.name, true) } ?: return null
			return group.subcommands.firstOrNull { subcommandName.equals(it.name, true) }
		}
	}
}

fun Command.getCommandMatchingPath(event: SlashCommandEvent): Command.Simple<*>? {
	return getCommandMatchingPath(event.name, event.subcommandGroup, event.subcommandName)
}