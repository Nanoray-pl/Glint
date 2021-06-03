package pl.nanoray.glint.bungie.api

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.custom.CharacterId
import pl.nanoray.glint.bungie.api.custom.ComponentResult

@Serializable
data class DestinyProfileResponse(
    val characters: ComponentResult<Map<CharacterId, DestinyCharacterComponent>>? = null,
    val characterEquipment: ComponentResult<Map<CharacterId, DestinyInventoryComponent>>? = null,
    val characterInventories: ComponentResult<Map<CharacterId, DestinyInventoryComponent>>? = null
)