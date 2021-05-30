@file:UseSerializers(UriSerializer::class)

package pl.nanoray.glint.bungie

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import pl.nanoray.glint.utilities.UriSerializer
import java.net.URI

@Serializable
internal data class Config(
	val oauth: OAuth,
	val apiKey: String
) {
	@Serializable
	data class OAuth(
		val clientId: String,
		val clientSecret: String,
		val redirectUrl: URI
	)
}