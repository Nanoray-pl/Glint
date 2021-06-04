package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ApiRelativeUrl

@Serializable
data class DestinyInventoryItemDefinition(
    val displayProperties: DestinyDisplayPropertiesDefinition,
    val screenshot: ApiRelativeUrl? = null,
    val flavorText: String? = null
)