package pl.nanoray.glint.slashcommand

import kotlinx.serialization.Serializable

@Serializable
data class Config(
	val globalCommandsAsGuildCommands: Boolean = false
)