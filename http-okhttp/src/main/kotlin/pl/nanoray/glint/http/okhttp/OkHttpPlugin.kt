package pl.nanoray.glint.http.okhttp

import pl.nanoray.glint.http.SingleHttpClientFactory
import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container

class OkHttpPlugin(
    container: Container
): ContainerEnabledPlugin(container) {
    init {
        register<SingleHttpClientFactory> { OkHttpClientFactory() }
    }
}