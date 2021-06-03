package pl.nanoray.glint.bungie.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ItemLocation.Serializer::class)
enum class ItemLocation(
    val id: Int
) {
    Unknown(0), Inventory(1), Vault(2), Vendor(3), Postmaster(4);

    object Serializer: KSerializer<ItemLocation> {
        private val intSerializer = Int.serializer()
        override val descriptor = intSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ItemLocation) {
            intSerializer.serialize(encoder, value.id)
        }

        override fun deserialize(decoder: Decoder): ItemLocation {
            val id = intSerializer.deserialize(decoder)
            return values().first { it.id == id }
        }
    }
}