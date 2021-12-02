package pl.nanoray.glint.spammoderation

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import pl.nanoray.glint.jdaextensions.MessageIdentifier
import pl.nanoray.glint.jdaextensions.TextChannelIdentifier
import pl.nanoray.glint.jdaextensions.UserIdentifier
import pl.nanoray.glint.jdaextensions.identifier
import pl.nanoray.glint.utilities.levenshtein
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.schedule
import kotlin.concurrent.withLock

internal interface SingleUserModerationFilter {
	fun addMessage(message: Message)
}

internal class SingleUserModerationFilterImpl(
	private val config: Config,
	val userIdentifier: UserIdentifier,
	private val discordModerationWorker: DiscordModerationWorker
): SingleUserModerationFilter {
	private data class BufferedMessage(
		val channelId: TextChannelIdentifier,
		val messageId: MessageIdentifier,
		val raw: String,
		val display: String,
		val stripped: String,
		val instant: Instant = Clock.System.now()
	)

	private val lock = ReentrantLock()
	private val timer = Timer()
	private var task: TimerTask? = null
	private val buffer = mutableListOf<BufferedMessage>()

	override fun addMessage(message: Message) {
		lock.withLock {
			buffer.add(BufferedMessage(
				(message.channel as TextChannel).identifier,
				message.identifier,
				message.contentRaw,
				message.contentDisplay,
				message.contentStripped
			))

			if (buffer.size > config.perUserBuffer.messageCount)
				buffer.removeFirst()
			if (buffer.size < config.rules.messageCount)
				return@withLock

			val contentToCompare = buffer.map {
				when (config.rules.matchMode) {
					Config.Rules.MatchMode.Raw -> it.raw
					Config.Rules.MatchMode.Display -> it.display
					Config.Rules.MatchMode.Stripped -> it.stripped
				}
			}
			val newContent = contentToCompare.last()
			val matchingMessages = contentToCompare.dropLast(1).filter {
				if (config.rules.messageEqualityThreshold <= 0)
					return@filter true
				if (config.rules.messageEqualityThreshold >= 1)
					return@filter false
				return@filter 1.0 - levenshtein(it, newContent).toDouble() / newContent.length.toDouble() >= config.rules.messageEqualityThreshold
			}
			if (matchingMessages.size + 1 < config.rules.messageCount)
				return@withLock

			when (val action = config.action) {
				Config.Action.Debug -> message.reply("This is a debug spam moderation action. Reason: spamming \"${message.contentDisplay}\"").queue()
				is Config.Action.Kick -> {
					discordModerationWorker.kickUser(
						message.author.identifier,
						(message.channel as TextChannel).guild.identifier,
						"Spam Filter: \"${message.contentDisplay}\""
					)
				}
				is Config.Action.Ban -> {
					discordModerationWorker.banUser(
						message.author.identifier,
						(message.channel as TextChannel).guild.identifier,
						action.daysOfMessagesToDelete,
						"Spam Filter: \"${message.contentDisplay}\""
					)
				}
			}
		}
	}

	private fun rescheduleTimer() {
		lock.withLock {
			task?.cancel()
			if (buffer.isEmpty())
				return@withLock
			val firstMessage = buffer.first()
			val scheduleInstant = firstMessage.instant + config.perUserBuffer.timeout
			val scheduleDelay = scheduleInstant.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()
			task = timer.schedule(scheduleDelay) {
				onTimerTrigger()
			}
		}
	}

	private fun onTimerTrigger() {
		lock.withLock {
			buffer.removeFirst()
			rescheduleTimer()
		}
	}
}