package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MembershipType.Serializer::class)
enum class MembershipType(
    val id: Int
) {
    None(0), Xbox(1), PSN(2), Steam(3), Blizzard(4), Stadia(5), Demon(10), BungieNext(254);

    object Serializer: KSerializer<MembershipType> {
        private val intSerializer = Int.serializer()
        override val descriptor = intSerializer.descriptor

        override fun serialize(encoder: Encoder, value: MembershipType) {
            intSerializer.serialize(encoder, value.id)
        }

        override fun deserialize(decoder: Decoder): MembershipType {
            val id = intSerializer.deserialize(decoder)
            return values().first { it.id == id }
        }
    }
}