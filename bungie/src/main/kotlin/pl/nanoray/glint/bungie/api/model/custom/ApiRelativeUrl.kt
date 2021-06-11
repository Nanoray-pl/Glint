package pl.nanoray.glint.bungie.api.model.custom

import kotlinx.serialization.Serializable
import java.net.URL

@Serializable
@JvmInline
value class ApiRelativeUrl(val path: String) {
    fun getUrl(baseUrl: URL): URL {
        return baseUrl.toURI().resolve(path).toURL()
    }
}