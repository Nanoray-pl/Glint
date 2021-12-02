package pl.nanoray.glint.http.oauth

import com.github.scribejava.core.model.OAuth2AccessToken
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface TokenParser<Token: OAuth2Token> {
	fun parseToken(apiToken: OAuth2AccessToken): Token
}

object SimpleTokenParser: TokenParser<SimpleOAuth2Token> {
	@Serializable
	private data class ExtraTokenInfo(
		@SerialName("refresh_expires_in") val refreshExpiresIn: Int? = null
	)

	private val jsonFormat = Json { ignoreUnknownKeys = true }

	override fun parseToken(apiToken: OAuth2AccessToken): SimpleOAuth2Token {
		val extraTokenInfo = jsonFormat.decodeFromString<ExtraTokenInfo>(apiToken.rawResponse)
		val now = Clock.System.now()
		return SimpleOAuth2Token(
			now,
			SingleToken(apiToken.accessToken, apiToken.expiresIn?.let { now + it.toDuration(DurationUnit.SECONDS) }),
			apiToken.refreshToken?.let { SingleToken(it, extraTokenInfo.refreshExpiresIn?.let { now + it.toDuration(DurationUnit.SECONDS) }) }
		)
	}
}