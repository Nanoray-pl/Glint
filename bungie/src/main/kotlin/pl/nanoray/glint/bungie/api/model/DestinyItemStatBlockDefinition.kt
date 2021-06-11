package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ManifestId

@Serializable
data class DestinyItemStatBlockDefinition(
    val stats: Map<ManifestId<DestinyStatDefinition>, DestinyInventoryItemStatDefinition>,
    val hasDisplayableStats: Boolean
)