package pl.nanoray.glint.bungie

import kotlinx.serialization.Serializable

@Serializable
internal data class Config(
	val oauth: OAuth
) {
	@Serializable
	internal data class OAuth(
		val clientId: String,
		val apiKey: String
	)
}