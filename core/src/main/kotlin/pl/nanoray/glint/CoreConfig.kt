package pl.nanoray.glint

import pl.nanoray.glint.jdaextensions.UserIdentifier

data class CoreConfig(
		val token: String,
		val owner: UserIdentifier
)