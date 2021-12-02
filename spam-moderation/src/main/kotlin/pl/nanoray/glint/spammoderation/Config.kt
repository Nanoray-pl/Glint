@file:UseSerializers(DurationSerializer::class)

package pl.nanoray.glint.spammoderation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import pl.nanoray.glint.utilities.DurationSerializer
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
internal data class Config(
	val perUserBuffer: PerUserBuffer = PerUserBuffer(),
	val rules: Rules = Rules(),
	val action: Action = Action.Ban()
) {
	@Serializable
	data class PerUserBuffer(
		val messageCount: Int = 5,
		val timeout: Duration = 2.toDuration(DurationUnit.MINUTES)
	)

	@Serializable
	data class Rules(
		val matchMode: MatchMode = MatchMode.Stripped,
		val messageCount: Int = 3,
		val messageEqualityThreshold: Double = 0.9
	) {
		@Serializable
		enum class MatchMode {
			Raw, Display, Stripped
		}
	}

	@Serializable
	sealed class Action {
		@Serializable
		@SerialName("Debug")
		object Debug: Action()

		@Serializable
		@SerialName("Kick")
		data class Kick(
			val removeMessages: MessageRemoving = MessageRemoving.Buffered
		): Action() {
			@Serializable
			enum class MessageRemoving {
				None, Matching, Buffered
			}
		}

		@Serializable
		@SerialName("Ban")
		data class Ban(
			val daysOfMessagesToDelete: Int = 1
		): Action()
	}
}