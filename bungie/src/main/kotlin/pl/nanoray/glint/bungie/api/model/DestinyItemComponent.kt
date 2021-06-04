package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ItemInstanceId
import pl.nanoray.glint.bungie.api.model.custom.ManifestId

@Serializable
data class DestinyItemComponent(
    val itemHash: ManifestId<DestinyInventoryItemDefinition>,
    val itemInstanceId: ItemInstanceId,
    val quantity: Int,
    val location: ItemLocation
)