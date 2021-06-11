package pl.nanoray.glint.http

import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container

class HttpPlugin(
    container: Container
): ContainerEnabledPlugin(container) {
    init {
        register<HttpRequestBuilder> { DefaultHttpRequestBuilder }
    }
}