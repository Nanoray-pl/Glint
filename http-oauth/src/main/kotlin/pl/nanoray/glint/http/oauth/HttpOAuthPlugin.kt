package pl.nanoray.glint.http.oauth

import pl.nanoray.glint.ConfigManager
import pl.nanoray.glint.getConfig
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.inject

class HttpOAuthPlugin(
	container: Container
): ContainerEnabledPlugin(container) {
	private val configManager: ConfigManager by resolver.inject()
	private val config by lazy { configManager.getConfig<Config>() ?: throw IllegalArgumentException("Cannot parse Config.") }

	private var redirectManager: OAuth2RedirectManager? = null

	init {
		register<OAuth2RedirectManager> {
			val redirectManager = OAuth2RedirectManagerImpl(config)
			this.redirectManager = redirectManager
			return@register redirectManager
		}
	}

	override fun onUnload() {
		super.onUnload()
		redirectManager?.close()
	}
}