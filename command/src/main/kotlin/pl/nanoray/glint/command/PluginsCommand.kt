package pl.nanoray.glint.command

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.plugin.PluginManager

internal class PluginsCommand(
		resolver: Resolver
): Command.WithSubcommands() {
	override val name = "plugin"
	override val description = "[Bot Owner] Manage bot plugins."
	override val subcommands = listOf(ListCommand(resolver), ReloadCommand(resolver))

	class ListCommand(
			resolver: Resolver
	): Command.Simple<Unit>(Unit::class) {
		override val name = "list"
		override val description = "[Bot Owner] List the current plugins."

		private val pluginManager: PluginManager.Dynamic.FullUnload.Reload by resolver.inject()

		override fun handleCommand(event: SlashCommandEvent, options: Unit) {
			event.reply(
					EmbedBuilder().apply {
						val loadedPlugins = pluginManager.loadedPluginInfos
						if (loadedPlugins.isNotEmpty())
							addField("Loaded plugins", loadedPlugins.joinToString("\n") { "${it.identifier}@${it.version}" }, false)

						val unloadedPlugins = pluginManager.unloadedPluginInfos
						if (unloadedPlugins.isNotEmpty())
							addField("Unloaded plugins", unloadedPlugins.joinToString("\n") { "${it.identifier}@${it.version}" }, false)
					}.build()
			).setEphemeral(true).queue()
		}
	}

	class ReloadCommand(
			resolver: Resolver
	): Command.Simple<Unit>(Unit::class) {
		override val name = "reload"
		override val description = "[Bot Owner] Reload the plugin list and all plugins."

		private val pluginManager: PluginManager.Dynamic.FullUnload.Reload by resolver.inject()

		override fun handleCommand(event: SlashCommandEvent, options: Unit) {
			event.acknowledge(true).flatMap { hook ->
				val oldPluginList = pluginManager.loadedPluginInfos.map { "${it.identifier}@${it.version}" }.toSet()
				pluginManager.unloadAllPluginsAndReloadPluginInfos()
				pluginManager.loadAllPlugins()
				val newPluginList = pluginManager.loadedPluginInfos.map { "${it.identifier}@${it.version}" }.toSet()

				val addedPlugins = newPluginList - oldPluginList
				val removedPlugins = oldPluginList - newPluginList
				hook.sendMessage(
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
				)
			}.queue()
		}
	}
}