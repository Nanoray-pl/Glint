package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import pl.nanoray.glint.bungie.api.model.custom.CharacterId
import pl.nanoray.glint.bungie.api.model.custom.ComponentResult

@Serializable
data class DestinyProfileResponse(
    val characters: ComponentResult<Map<CharacterId, DestinyCharacterComponent>>? = null,
    val characterInventories: ComponentResult<Map<CharacterId, DestinyInventoryComponent>>? = null,
    val characterEquipment: ComponentResult<Map<CharacterId, DestinyInventoryComponent>>? = null
) {
    enum class Component(
        val id: Int
    ) {
        Characters(200), CharacterInventories(201), CharacterEquipment(205);

        object Serializer: KSerializer<Component> {
            private val intSerializer = Int.serializer()
            override val descriptor = intSerializer.descriptor

            override fun serialize(encoder: Encoder, value: Component) {
                intSerializer.serialize(encoder, value.id)
            }

            override fun deserialize(decoder: Decoder): Component {
                val id = intSerializer.deserialize(decoder)
                return Component.values().first { it.id == id }
            }
        }
    }
}