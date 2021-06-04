package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ApiRelativeUrl

@Serializable
data class DestinyManifest(
    val version: String,
    val jsonWorldComponentContentPaths: Map<String, JsonWorldComponentContentPaths>
) {
    @Serializable
    data class JsonWorldComponentContentPaths(
        @SerialName("DestinyStatDefinition") val stat: ApiRelativeUrl,
        @SerialName("DestinyInventoryItemDefinition") val inventoryItem: ApiRelativeUrl
    )
}