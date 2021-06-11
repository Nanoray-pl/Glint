package pl.nanoray.glint.bungie.api.service.http

import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.json.Json
import pl.nanoray.glint.bungie.api.model.DestinyManifest
import pl.nanoray.glint.bungie.api.service.ManifestService
import pl.nanoray.glint.http.*
import java.net.URL

class HttpManifestService(
    client: SingleHttpClient,
    requestBuilder: HttpRequestBuilder,
    jsonFormat: Json = Json { ignoreUnknownKeys = true },
    private val getManifest: Endpoint<Void, Void>
): BaseHttpBungieService(client, requestBuilder, jsonFormat), ManifestService {
    constructor(
        client: SingleHttpClient,
        requestBuilder: HttpRequestBuilder,
        jsonFormat: Json = Json { ignoreUnknownKeys = true },
        baseUrl: URL
    ): this(
        client, requestBuilder, jsonFormat,
        Endpoint(HttpRequest.Method.GET, EndpointUrl(baseUrl.toURI().resolve("/Platform/Destiny2/Manifest").toURL()))
    )

    override fun getManifest(): Single<DestinyManifest> {
        return client.requestSingle(requestBuilder.buildRequest(
            getManifest.method,
            getManifest.url.create()
        )).deserialize()
    }
}