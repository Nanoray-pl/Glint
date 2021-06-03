package pl.nanoray.glint.bungie.api

import kotlinx.serialization.Serializable

@Serializable
data class DestinyInventoryComponent(
    val items: List<DestinyItemComponent>
)