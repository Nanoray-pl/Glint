package pl.nanoray.glint.http.okhttp

import pl.nanoray.glint.http.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.time.toJavaDuration
import okhttp3.OkHttpClient as Client

internal class OkHttpClientFactory: SingleHttpClientFactory {
    override fun createHttpClient(configuration: Map<HttpClientConfigurationKey, Any>): SingleHttpClient {
        val client = Client.Builder()
            .connectTimeout((configuration[HttpClientConnectTimeout] as? Duration ?: 10.toDuration(DurationUnit.SECONDS)).toJavaDuration())
            .readTimeout((configuration[HttpClientReadTimeout] as? Duration ?: 10.toDuration(DurationUnit.SECONDS)).toJavaDuration())
            .writeTimeout((configuration[HttpClientWriteTimeout] as? Duration ?: 10.toDuration(DurationUnit.SECONDS)).toJavaDuration())
            .callTimeout((configuration[HttpClientCallTimeout] as? Duration ?: 10.toDuration(DurationUnit.SECONDS)).toJavaDuration())
            .build()
        return OkHttpClient(client)
    }
}