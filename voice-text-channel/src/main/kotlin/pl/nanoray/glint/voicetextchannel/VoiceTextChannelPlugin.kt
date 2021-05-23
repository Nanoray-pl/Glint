package pl.nanoray.glint.voicetextchannel

import net.dv8tion.jda.api.JDA
import pl.nanoray.glint.DurationParser
import pl.nanoray.glint.messagecommand.MessageCommandManager
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.nanoray.glint.store.ConfigStore
import pl.nanoray.glint.store.StorePlugin
import pl.nanoray.glint.store.replacingNull
import pl.nanoray.glint.store.throttling
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.dependency.register

class VoiceTextChannelPlugin(
	container: Container
): ContainerEnabledPlugin(container) {
	private val jda: JDA by resolver.inject()
	private val durationParser: DurationParser by resolver.inject()
	private val messageCommandManager: MessageCommandManager by resolver.inject()
	private val storePlugin: StorePlugin by resolver.inject()

	private val pluginContainer = Container(resolver)
	private val discordWorker: DiscordWorker by pluginContainer.inject()
	private val eventListener by lazy { DiscordEventListener(pluginContainer) }

	private val command = VoiceTextCommand(resolver)
	private val mappingStore = ConfigStore<Set<ChannelMapping>>(resolver, "${this::class.qualifiedName!!}.mappings")
		.replacingNull(emptySet())
		.throttling(15_000)

	init {
		register<WritableVoiceTextChannelManager> { VoiceTextChannelManagerImpl(it, mappingStore) }
		pluginContainer.register<DiscordWorker> { DiscordWorkerImpl(it) }

		jda.addEventListener(eventListener)
		messageCommandManager.registerMessageCommand(command)
		storePlugin.registerThrottleStore(mappingStore)

		if (jda.status == JDA.Status.CONNECTED)
			discordWorker.cleanUpStaleVoiceTextChannelMappings().subscribe()
	}

	override fun onUnload() {
		jda.removeEventListener(eventListener)
		messageCommandManager.unregisterMessageCommand(command)
		storePlugin.unregisterThrottleStore(mappingStore)
		super.onUnload()
	}
}