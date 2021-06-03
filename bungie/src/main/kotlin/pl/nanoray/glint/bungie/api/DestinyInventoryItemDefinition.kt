package pl.nanoray.glint.bungie.api

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.custom.ApiRelativeUrl

@Serializable
data class DestinyInventoryItemDefinition(
    val displayProperties: DestinyDisplayPropertiesDefinition,
    val screenshot: ApiRelativeUrl? = null,
    val flavorText: String? = null
)