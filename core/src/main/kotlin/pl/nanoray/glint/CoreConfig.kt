package pl.nanoray.glint

import kotlinx.serialization.Serializable
import pl.nanoray.glint.jdaextensions.UserIdentifier

@Serializable
data class CoreConfig(
		val token: String,
		val owner: UserIdentifier
)