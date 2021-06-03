package pl.nanoray.glint.bungie.api

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.custom.ItemInstanceId
import pl.nanoray.glint.bungie.api.custom.ManifestId

@Serializable
data class DestinyItemComponent(
    val itemHash: ManifestId<DestinyInventoryItemDefinition>,
    val itemInstanceId: ItemInstanceId,
    val quantity: Int,
    val location: ItemLocation
)