package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ManifestId

@Serializable
data class DestinyInventoryItemStatDefinition(
    val statHash: ManifestId<DestinyInventoryItemStatDefinition>,
    val value: Int,
    val minimum: Int,
    val maximum: Int,
    val displayMaximum: Int? = null
)