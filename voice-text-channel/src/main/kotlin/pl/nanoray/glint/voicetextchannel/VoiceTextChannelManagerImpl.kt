package pl.nanoray.glint.voicetextchannel

import io.reactivex.rxjava3.core.Completable
import pl.nanoray.glint.jdaextensions.TextChannelIdentifier
import pl.nanoray.glint.jdaextensions.VoiceChannelIdentifier
import pl.nanoray.glint.utilities.WithDefault
import pl.shockah.unikorn.collection.removeFirst
import pl.shockah.unikorn.dependency.Resolver
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class VoiceTextChannelManagerImpl(
		private val resolver: Resolver
): WritableVoiceTextChannelManager {
	private val lock = ReentrantReadWriteLock()
	private val observers = mutableListOf<VoiceTextChannelManagerObserver>()
	private val mappings = mutableSetOf<ChannelMapping>()

	override fun addVoiceTextChannelObserver(observer: VoiceTextChannelManagerObserver) {
		lock.write { observers.add(observer) }
	}

	override fun removeVoiceTextChannelObserver(observer: VoiceTextChannelManagerObserver) {
		lock.write { observers.remove(observer) }
	}

	override fun getVoiceTextChannelMappings(): Set<ChannelMapping> {
		return lock.read { mappings.toSet() }
	}

	override fun getMappingForVoiceChannel(voiceChannel: VoiceChannelIdentifier): ChannelMapping? {
		return lock.read { mappings.firstOrNull { it.voiceChannel == voiceChannel } }
	}

	override fun getMappingForTextChannel(textChannel: TextChannelIdentifier): ChannelMapping? {
		return lock.read { mappings.firstOrNull { it.voiceChannel == textChannel } }
	}

	override fun linkTextChannelToVoiceChannel(
			textChannel: TextChannelIdentifier,
			voiceChannel: VoiceChannelIdentifier,
			configuration: WithDefault<ChannelMapping.Configuration>
	): Completable {
		return Completable.defer {
			lock.write {
				val textChannelMapping = mappings.removeFirst { it.textChannel == textChannel }
				val voiceChannelMapping = mappings.removeFirst { it.voiceChannel == voiceChannel }

				if (textChannelMapping != null)
					observers.forEach { it.onVoiceTextChannelMappingRemoved(this, textChannelMapping) }
				if (voiceChannelMapping != null)
					observers.forEach { it.onVoiceTextChannelMappingRemoved(this, voiceChannelMapping) }

				val newMapping = ChannelMapping(
						voiceChannel,
						textChannel,
						configuration.valueOrDefault(VoiceTextChannelDefaults.mappingConfiguration)
				)
				mappings.add(newMapping)
				observers.forEach { it.onVoiceTextChannelMappingAdded(this, newMapping) }
				return@write Completable.complete()
				// TODO: Reset permission overrides
			}
		}
	}

	override fun unlinkTextChannelFromVoiceChannel(textChannel: TextChannelIdentifier): Completable {
		return Completable.defer {
			lock.write {
				val mapping = mappings.removeFirst { it.textChannel == textChannel } ?: return@write Completable.complete()
				observers.forEach { it.onVoiceTextChannelMappingRemoved(this, mapping) }
				return@write Completable.complete()
				// TODO: Reset permission overrides
			}
		}
	}

	override fun unlinkVoiceChannelFromTextChannel(voiceChannel: VoiceChannelIdentifier): Completable {
		return Completable.defer {
			lock.write {
				val mapping = mappings.removeFirst { it.voiceChannel == voiceChannel } ?: return@write Completable.complete()
				observers.forEach { it.onVoiceTextChannelMappingRemoved(this, mapping) }
				return@write Completable.complete()
				// TODO: Reset permission overrides
			}
		}
	}
}