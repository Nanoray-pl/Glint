package pl.nanoray.glint.voicetextchannel

import io.reactivex.rxjava3.core.Completable
import net.dv8tion.jda.api.JDA
import pl.nanoray.glint.jdaextensions.TextChannelIdentifier
import pl.nanoray.glint.jdaextensions.VoiceChannelIdentifier
import pl.nanoray.glint.jdaextensions.getVoiceChannel
import pl.nanoray.glint.store.MutableStore
import pl.nanoray.glint.utilities.WithDefault
import pl.shockah.unikorn.collection.removeFirst
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.CheckReturnValue
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration

internal class VoiceTextChannelManagerImpl(
	private val resolver: Resolver,
	private val store: MutableStore<Set<ChannelMapping>>
): WritableVoiceTextChannelManager {
	private val jda: JDA by resolver.inject()
	private val discordWorker: DiscordWorker by resolver.inject()

	private val lock = ReentrantReadWriteLock()
	private val observers = mutableListOf<VoiceTextChannelManagerObserver>()

	override val voiceTextChannelMappings: Set<ChannelMapping>
		get() = lock.read { store.value.toSet() }

	override fun addVoiceTextChannelObserver(observer: VoiceTextChannelManagerObserver) {
		lock.write { observers.add(observer) }
	}

	override fun removeVoiceTextChannelObserver(observer: VoiceTextChannelManagerObserver) {
		lock.write { observers.remove(observer) }
	}

	override fun getMappingForVoiceChannel(voiceChannel: VoiceChannelIdentifier): ChannelMapping? {
		return lock.read { store.value.firstOrNull { it.voiceChannel == voiceChannel } }
	}

	override fun getMappingForTextChannel(textChannel: TextChannelIdentifier): ChannelMapping? {
		return lock.read { store.value.firstOrNull { it.voiceChannel == textChannel } }
	}

	@CheckReturnValue
	override fun linkTextChannelToVoiceChannel(
		textChannel: TextChannelIdentifier,
		voiceChannel: VoiceChannelIdentifier,
		duration: WithDefault<Duration>
	): Completable {
		return Completable.defer {
			lock.write {
				val mappings = store.value.toMutableSet()
				val textChannelMapping = mappings.removeFirst { it.textChannel == textChannel }
				val voiceChannelMapping = mappings.removeFirst { it.voiceChannel == voiceChannel }
				store.value = mappings

				if (textChannelMapping != null)
					observers.forEach { it.onVoiceTextChannelMappingRemoved(this, textChannelMapping) }
				if (voiceChannelMapping != null)
					observers.forEach { it.onVoiceTextChannelMappingRemoved(this, voiceChannelMapping) }

				val newMapping = ChannelMapping(
					voiceChannel,
					textChannel,
					duration.valueOrDefault(VoiceTextChannelDefaults.historyDuration)
				)
				mappings.add(newMapping)
				observers.forEach { it.onVoiceTextChannelMappingAdded(this, newMapping) }
				val voiceChannelEntity = jda.getVoiceChannel(voiceChannel) ?: return@write Completable.complete()
				return@write Completable.merge(voiceChannelEntity.members.map { discordWorker.grantAccess(newMapping, voiceChannelEntity.guild, it) })
			}
		}
	}

	@CheckReturnValue
	override fun unlinkTextChannelFromVoiceChannel(textChannel: TextChannelIdentifier): Completable {
		return Completable.defer {
			lock.write {
				val mappings = store.value.toMutableSet()
				val mapping = mappings.removeFirst { it.textChannel == textChannel } ?: return@write Completable.complete()
				store.value = mappings
				observers.forEach { it.onVoiceTextChannelMappingRemoved(this, mapping) }
				val voiceChannelEntity = jda.getVoiceChannel(mapping.voiceChannel) ?: return@write Completable.complete()
				return@write Completable.merge(voiceChannelEntity.members.map { discordWorker.denyAccess(mapping, voiceChannelEntity.guild, it) })
			}
		}
	}

	@CheckReturnValue
	override fun unlinkVoiceChannelFromTextChannel(voiceChannel: VoiceChannelIdentifier): Completable {
		return Completable.defer {
			lock.write {
				val mappings = store.value.toMutableSet()
				val mapping = mappings.removeFirst { it.voiceChannel == voiceChannel } ?: return@write Completable.complete()
				store.value = mappings
				observers.forEach { it.onVoiceTextChannelMappingRemoved(this, mapping) }
				val voiceChannelEntity = jda.getVoiceChannel(mapping.voiceChannel) ?: return@write Completable.complete()
				return@write Completable.merge(voiceChannelEntity.members.map { discordWorker.denyAccess(mapping, voiceChannelEntity.guild, it) })
			}
		}
	}
}