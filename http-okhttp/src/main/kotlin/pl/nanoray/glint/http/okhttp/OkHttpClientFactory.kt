package pl.nanoray.glint.http.okhttp

import pl.nanoray.glint.http.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.toDuration
import kotlin.time.toJavaDuration
import okhttp3.OkHttpClient as Client

internal class OkHttpClientFactory: SingleHttpClientFactory {
    override fun createHttpClient(configuration: Map<HttpClientConfigurationKey, Any>): SingleHttpClient {
        val client = Client.Builder()
            .connectTimeout((configuration[HttpClientConnectTimeout] as? Duration ?: 10.toDuration(TimeUnit.SECONDS)).toJavaDuration())
            .readTimeout((configuration[HttpClientReadTimeout] as? Duration ?: 10.toDuration(TimeUnit.SECONDS)).toJavaDuration())
            .writeTimeout((configuration[HttpClientWriteTimeout] as? Duration ?: 10.toDuration(TimeUnit.SECONDS)).toJavaDuration())
            .callTimeout((configuration[HttpClientCallTimeout] as? Duration ?: 10.toDuration(TimeUnit.SECONDS)).toJavaDuration())
            .build()
        return OkHttpClient(client)
    }
}