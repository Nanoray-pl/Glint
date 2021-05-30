package pl.nanoray.glint.http.oauth

import kotlinx.serialization.Serializable

@Serializable
internal data class Config(
	val redirectServerPort: Int
)