package pl.nanoray.glint.voicetextchannel

import io.reactivex.rxjava3.core.Completable
import pl.nanoray.glint.jdaextensions.TextChannelIdentifier
import pl.nanoray.glint.jdaextensions.VoiceChannelIdentifier
import pl.nanoray.glint.store.Store
import pl.nanoray.glint.utilities.WithDefault
import pl.shockah.unikorn.collection.removeFirst
import pl.shockah.unikorn.dependency.Resolver
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.CheckReturnValue
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration

internal class VoiceTextChannelManagerImpl(
		private val resolver: Resolver,
		private val store: Store<Set<ChannelMapping>>
): WritableVoiceTextChannelManager {
	private val lock = ReentrantReadWriteLock()
	private val observers = mutableListOf<VoiceTextChannelManagerObserver>()

	override val voiceTextChannelMappings: Set<ChannelMapping>
		get() = lock.read { store.value }

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
				return@write Completable.complete()
				// TODO: Reset permission overrides
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
				return@write Completable.complete()
				// TODO: Reset permission overrides
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
				return@write Completable.complete()
				// TODO: Reset permission overrides
			}
		}
	}
}