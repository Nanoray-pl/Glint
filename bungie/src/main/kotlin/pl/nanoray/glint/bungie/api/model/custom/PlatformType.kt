package pl.nanoray.glint.bungie.api.model.custom

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = PlatformType.Serializer::class)
enum class PlatformType(
    val id: Int
) {
    Unknown(0), PlayStation(1), Xbox(2), Blizzard(3), Steam(4), Stadia(5);

    object Serializer: KSerializer<PlatformType> {
        private val intSerializer = Int.serializer()
        override val descriptor = intSerializer.descriptor

        override fun serialize(encoder: Encoder, value: PlatformType) {
            intSerializer.serialize(encoder, value.id)
        }

        override fun deserialize(decoder: Decoder): PlatformType {
            val id = intSerializer.deserialize(decoder)
            return values().first { it.id == id }
        }
    }
}