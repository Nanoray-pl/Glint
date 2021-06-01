package pl.nanoray.glint.bungie

import com.github.scribejava.core.model.OAuth2AccessToken
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import pl.nanoray.glint.http.oauth.OAuth2Token
import pl.nanoray.glint.http.oauth.SingleToken
import pl.nanoray.glint.http.oauth.TokenParser
import java.util.concurrent.TimeUnit
import kotlin.time.toDuration

@Serializable
data class BungieToken(
	override val creationTime: Instant,
	override val accessToken: SingleToken,
	override val refreshToken: SingleToken?,
	val membershipId: String
): OAuth2Token

object BungieTokenParser: TokenParser<BungieToken> {
	@Serializable
	private data class ExtraTokenInfo(
		@SerialName("refresh_expires_in") val refreshExpiresIn: Int? = null,
		@SerialName("membership_id") val membershipId: String
	)

	private val jsonFormat = Json { ignoreUnknownKeys = true }

	override fun parseToken(apiToken: OAuth2AccessToken): BungieToken {
		val extraTokenInfo = jsonFormat.decodeFromString<ExtraTokenInfo>(apiToken.rawResponse)
		val now = Clock.System.now()
		return BungieToken(
			now,
			SingleToken(apiToken.accessToken, apiToken.expiresIn?.let { now + it.toDuration(TimeUnit.SECONDS) }),
			apiToken.refreshToken?.let { SingleToken(it, extraTokenInfo.refreshExpiresIn?.let { now + it.toDuration(
				TimeUnit.SECONDS) }) },
			extraTokenInfo.membershipId
		)
	}
}