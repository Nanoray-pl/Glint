package pl.nanoray.glint.bungie

import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.oauth.OAuth20Service
import pl.nanoray.glint.ConfigManager
import pl.nanoray.glint.bungie.api.service.ManifestService
import pl.nanoray.glint.bungie.api.service.http.HttpManifestService
import pl.nanoray.glint.getConfig
import pl.nanoray.glint.http.HttpRequestBuilder
import pl.nanoray.glint.http.SingleHttpClientFactory
import pl.nanoray.glint.http.oauth.OAuth2RedirectManager
import pl.nanoray.glint.http.oauth.OAuth2SingleHttpClientManager
import pl.nanoray.glint.http.oauth.StoreOAuth2SingleHttpClientManager
import pl.nanoray.glint.http.oauth.TokenParser
import pl.nanoray.glint.jdaextensions.UserIdentifier
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.nanoray.glint.slashcommand.SlashCommand
import pl.nanoray.glint.slashcommand.SlashCommandManager
import pl.nanoray.glint.slashcommand.SlashCommandProvider
import pl.nanoray.glint.store.ConfigStore
import pl.nanoray.glint.store.StorePlugin
import pl.nanoray.glint.store.map.toMutableMapStore
import pl.nanoray.glint.store.replacingNull
import pl.nanoray.glint.store.throttling
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject
import pl.shockah.unikorn.dependency.register

class BungiePlugin(
	container: Container
): ContainerEnabledPlugin(container), SlashCommandProvider {
	private val configManager: ConfigManager by resolver.inject()
	private val slashCommandManager: SlashCommandManager by resolver.inject()
	private val httpClientFactory: SingleHttpClientFactory by resolver.inject()
	private val httpRequestBuilder: HttpRequestBuilder by resolver.inject()
	private val redirectManager: OAuth2RedirectManager by resolver.inject()
	private val storePlugin: StorePlugin by resolver.inject()

	private val config by lazy { configManager.getConfig<Config>() ?: throw IllegalArgumentException("Cannot parse Config.") }
	private val service: OAuth20Service by lazy { ServiceBuilder(config.oauth.clientId).apiSecret(config.oauth.clientSecret).callback("${config.oauth.redirectUrl}").build(BungieOAuthApi()) }
	private val httpClient by lazy { httpClientFactory.createHttpClient() }

	private val pluginContainer = Container(container)
	private val tokenParser: TokenParser<BungieToken> by pluginContainer.inject()
	private val httpClientManager: OAuth2SingleHttpClientManager<UserIdentifier, BungieToken> by pluginContainer.inject()
	private val manifestService: ManifestService by pluginContainer.inject()
	private val manifestManager: ManifestManager by pluginContainer.inject()

	private val tokenStore = ConfigStore<Map<UserIdentifier, BungieToken>>(resolver, "${this::class.qualifiedName!!}.tokens")
		.replacingNull(emptyMap())
		.throttling(15_000)

	private val bungieLinkCommand = BungieLinkCommand(pluginContainer)

	override val globalSlashCommands: Set<SlashCommand>
		get() = setOf(bungieLinkCommand)

	init {
		register<ManifestManager> { LatestVersionWithDiskStorageManifestManager(config.manifestStoragePath, httpRequestBuilder, httpClient, manifestService, config.apiUrl) }
		pluginContainer.register<TokenParser<BungieToken>> { BungieTokenParser }
		pluginContainer.register<OAuth2SingleHttpClientManager<UserIdentifier, BungieToken>> { StoreOAuth2SingleHttpClientManager(httpClient, tokenStore.toMutableMapStore(), redirectManager, service, tokenParser) }
		pluginContainer.register<ManifestService> { HttpManifestService(httpClient, httpRequestBuilder, baseUrl = config.apiUrl) }
		slashCommandManager.registerSlashCommandProvider(this)
		storePlugin.registerThrottleStore(tokenStore)

		manifestManager // Initialize
	}

	override fun onUnload() {
		slashCommandManager.unregisterSlashCommandProvider(this)
		storePlugin.unregisterThrottleStore(tokenStore)
		super.onUnload()
	}
}