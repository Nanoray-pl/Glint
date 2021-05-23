package pl.nanoray.glint.http.okhttp

import pl.nanoray.glint.http.*
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import okhttp3.OkHttpClient as Client

internal class OkHttpClientFactory: SingleHttpClientFactory {
    override fun createHttpClient(configuration: Map<HttpClientConfigurationKey, Any>): SingleHttpClient {
        val client = Client.Builder()
            .connectTimeout((configuration[HttpClientConnectTimeout] as? Duration ?: Duration.seconds(10)).toJavaDuration())
            .readTimeout((configuration[HttpClientReadTimeout] as? Duration ?: Duration.seconds(10)).toJavaDuration())
            .writeTimeout((configuration[HttpClientWriteTimeout] as? Duration ?: Duration.seconds(10)).toJavaDuration())
            .callTimeout((configuration[HttpClientCallTimeout] as? Duration ?: Duration.seconds(0)).toJavaDuration())
            .build()
        return OkHttpClient(client)
    }
}