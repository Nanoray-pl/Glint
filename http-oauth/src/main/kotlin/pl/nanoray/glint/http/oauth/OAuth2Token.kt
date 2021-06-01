package pl.nanoray.glint.http.oauth

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SingleToken(
	val token: String,
	val expiryTime: Instant?
)

interface OAuth2Token {
	val creationTime: Instant
	val accessToken: SingleToken
	val refreshToken: SingleToken?
}

@Serializable
data class SimpleOAuth2Token(
	override val creationTime: Instant,
	override val accessToken: SingleToken,
	override val refreshToken: SingleToken?
): OAuth2Token