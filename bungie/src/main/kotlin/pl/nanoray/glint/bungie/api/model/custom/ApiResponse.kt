package pl.nanoray.glint.bungie.api.model.custom

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.toDuration

@Serializable
data class ApiResponse<Response>(
    @SerialName("Response") val response: Response? = null,
    @SerialName("ErrorCode") val errorCode: Int,
    @SerialName("ThrottleSeconds") @Serializable(with = DurationSecondsSerializer::class) val throttle: Duration,
    @SerialName("ErrorStatus") val errorStatus: String,
    @SerialName("Message") val message: String,
    @SerialName("MessageData") val messageData: Map<String, String>
) {
    private object DurationSecondsSerializer: KSerializer<Duration> {
        private val intSerializer = Int.serializer()
        override val descriptor = intSerializer.descriptor

        override fun serialize(encoder: Encoder, value: Duration) {
            intSerializer.serialize(encoder, value.inWholeSeconds.toInt())
        }

        override fun deserialize(decoder: Decoder): Duration {
            return intSerializer.deserialize(decoder).toDuration(TimeUnit.SECONDS)
        }
    }
}