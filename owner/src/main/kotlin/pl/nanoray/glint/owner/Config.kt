package pl.nanoray.glint.owner

import kotlinx.serialization.Serializable
import pl.nanoray.glint.jdaextensions.UserIdentifier

@Serializable
data class Config(
		val owners: Set<UserIdentifier>
)