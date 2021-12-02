package pl.nanoray.glint.bungie.api.service.http

import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import pl.nanoray.glint.bungie.api.model.custom.ApiResponse
import pl.nanoray.glint.http.HttpRequestBuilder
import pl.nanoray.glint.http.HttpResponse
import pl.nanoray.glint.http.SingleHttpClient

abstract class BaseHttpBungieService(
    protected val client: SingleHttpClient,
    protected val requestBuilder: HttpRequestBuilder,
    protected val jsonFormat: Json
) {
    class BungieResponseErrorException(
        val errorCode: Int
    ): Exception()

    protected inline fun <reified Response: Any, reified T: ApiResponse<Response>> Single<HttpResponse>.deserialize(): Single<Response> {
        return map { jsonFormat.decodeFromString(serializer<T>(), String(it.data)) }
            .flatMap {
                return@flatMap when {
                    it.errorCode != 1 -> Single.error(BungieResponseErrorException(it.errorCode))
                    it.response == null -> Single.error(BungieResponseErrorException(it.errorCode))
                    else -> Single.just(it.response)
                }
            }
    }
}