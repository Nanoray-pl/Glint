package pl.nanoray.glint.bungie.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = DestinyStatCategory.Serializer::class)
enum class DestinyStatCategory(
    val id: Int
) {
    Gameplay(0), Weapon(1), Defense(2), Primary(3);

    object Serializer: KSerializer<DestinyStatCategory> {
        private val intSerializer = Int.serializer()
        override val descriptor = intSerializer.descriptor

        override fun serialize(encoder: Encoder, value: DestinyStatCategory) {
            intSerializer.serialize(encoder, value.id)
        }

        override fun deserialize(decoder: Decoder): DestinyStatCategory {
            val id = intSerializer.deserialize(decoder)
            return values().first { it.id == id }
        }
    }
}