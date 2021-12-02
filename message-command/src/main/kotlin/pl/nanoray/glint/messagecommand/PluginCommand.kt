package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import pl.nanoray.glint.command.CommandPredicate
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.plugin.PluginManager
import kotlin.reflect.typeOf

internal class PluginCommand(
	resolver: Resolver
): MessageCommand<Unit>(typeOf<Unit>()) {
	override val name = "plugin"
	override val description = "List the current plugins."
	override val subcommands = listOf(Reload())
	override val predicates = listOf(CommandPredicate.IsOwner(resolver))

	private val pluginManager: PluginManager.Dynamic.FullUnload.Reload by resolver.inject()

	override fun handleCommand(message: Message, options: Unit) {
		message.reply(
			MessageBuilder(
				EmbedBuilder().apply {
					val loadedPlugins = pluginManager.loadedPluginInfos
					if (loadedPlugins.isNotEmpty())
						addField("Loaded plugins", loadedPlugins.joinToString("\n") { "${it.identifier}@${it.version}" }, false)

					val unloadedPlugins = pluginManager.unloadedPluginInfos
					if (unloadedPlugins.isNotEmpty())
						addField("Unloaded plugins", unloadedPlugins.joinToString("\n") { "${it.identifier}@${it.version}" }, false)
				}.build()
			).build()
		).queue()
	}

	inner class Reload: MessageCommand<Unit>(typeOf<Unit>()) {
		override val name = "reload"
		override val description = "Reload the plugin list and all plugins."

		override fun handleCommand(message: Message, options: Unit) {
			val oldPluginList = pluginManager.loadedPluginInfos.map { "${it.identifier}@${it.version}" }.toSet()
			pluginManager.unloadAllPluginsAndReloadPluginInfos()
			pluginManager.loadAllPlugins()
			val newPluginList = pluginManager.loadedPluginInfos.map { "${it.identifier}@${it.version}" }.toSet()

			val addedPlugins = newPluginList - oldPluginList
			val removedPlugins = oldPluginList - newPluginList
			message.reply(
				MessageBuilder(
					EmbedBuilder().apply {
						appendDescription("Reload finished.")
						if (removedPlugins.isNotEmpty())
							addField("Removed plugins", removedPlugins.joinToString("\n"), false)
						if (addedPlugins.isNotEmpty())
							addField("Added plugins", addedPlugins.joinToString("\n"), false)
						addField(
							"Plugin count",
							if (newPluginList.size == oldPluginList.size) "${newPluginList.size}" else "${oldPluginList.size} → ${newPluginList.size}",
							false
						)
					}.build()
				).build()
			).queue()
		}
	}
}