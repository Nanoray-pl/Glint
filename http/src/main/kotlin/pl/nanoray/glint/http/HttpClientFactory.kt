package pl.nanoray.glint.http

interface SingleHttpClientFactory {
    fun createHttpClient(configuration: Map<HttpClientConfigurationKey, Any> = emptyMap()): SingleHttpClient
}

interface ObservableHttpClientFactory: SingleHttpClientFactory {
    override fun createHttpClient(configuration: Map<HttpClientConfigurationKey, Any>): ObservableHttpClient
}