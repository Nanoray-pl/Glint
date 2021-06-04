package pl.nanoray.glint.bungie.api.service.http

import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.json.Json
import pl.nanoray.glint.bungie.api.model.DestinyManifest
import pl.nanoray.glint.bungie.api.service.ManifestService
import pl.nanoray.glint.http.Endpoint
import pl.nanoray.glint.http.HttpRequestBuilder
import pl.nanoray.glint.http.SingleHttpClient
import pl.nanoray.glint.http.create

class HttpManifestService(
    client: SingleHttpClient,
    requestBuilder: HttpRequestBuilder,
    jsonFormat: Json,
    private val getManifest: Endpoint<Void, Void>
): BaseHttpBungieService(client, requestBuilder, jsonFormat), ManifestService {
    override fun getManifest(): Single<DestinyManifest> {
        return client.requestSingle(requestBuilder.buildRequest(
            getManifest.method,
            getManifest.url.create()
        )).deserialize()
    }
}