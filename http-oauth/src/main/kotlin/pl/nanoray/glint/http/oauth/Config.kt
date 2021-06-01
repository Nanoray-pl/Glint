@file:UseSerializers(DurationSerializer::class)

package pl.nanoray.glint.http.oauth

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import pl.nanoray.glint.utilities.DurationSerializer
import java.nio.file.Path
import kotlin.io.path.Path

@Serializable
data class Config(
	val redirectServer: RedirectServer
) {
	@Serializable
	data class RedirectServer(
		val port: Int,
		val ssl: SSL? = null
	) {
		@Serializable
		data class SSL(
			val keystore: Keystore
		) {
			@Serializable
			data class Keystore(
				val path: Path = Path("config/keystore.jks"),
				val password: String
			)
		}
	}
}