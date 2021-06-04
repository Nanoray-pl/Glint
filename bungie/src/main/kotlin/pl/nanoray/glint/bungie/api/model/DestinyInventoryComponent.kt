package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.Serializable

@Serializable
data class DestinyInventoryComponent(
    val items: List<DestinyItemComponent>
)