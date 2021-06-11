@file:UseSerializers(UrlSerializer::class, UriSerializer::class)

package pl.nanoray.glint.bungie

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import pl.nanoray.glint.utilities.UriSerializer
import pl.nanoray.glint.utilities.UrlSerializer
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

@Serializable
internal data class Config(
	val oauth: OAuth,
	val apiUrl: URL = URL("https://www.bungie.net/"),
	val apiKey: String,
	val manifestStoragePath: Path = Paths.get("destiny2Manifest")
) {
	@Serializable
	data class OAuth(
		val clientId: String,
		val clientSecret: String,
		val redirectUrl: URI
	)
}