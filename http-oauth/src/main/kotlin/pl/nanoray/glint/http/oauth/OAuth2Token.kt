package pl.nanoray.glint.http.oauth

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class OAuth2Token(
	val accessToken: String,
	val refreshToken: String?,
	val expiryTime: Instant
)