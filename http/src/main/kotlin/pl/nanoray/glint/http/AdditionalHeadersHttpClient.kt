package pl.nanoray.glint.http

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

fun ObservableHttpClient.withAdditionalHeaders(headers: Map<String, String>): ObservableHttpClient {
    return AdditionalHeadersObservableHttpClient(this, headers)
}

fun SingleHttpClient.withAdditionalHeaders(headers: Map<String, String>): SingleHttpClient {
    return AdditionalHeadersSingleHttpClient(this, headers)
}

class AdditionalHeadersObservableHttpClient(
    private val wrapped: ObservableHttpClient,
    val headers: Map<String, String>
): ObservableHttpClient {
    override fun requestObservable(request: HttpRequest): Observable<HttpTaskOutput<HttpResponse>> {
        val newRequest = request.copy(headers = request.headers + headers)
        return wrapped.requestObservable(newRequest)
    }
}

class AdditionalHeadersSingleHttpClient(
    private val wrapped: SingleHttpClient,
    val headers: Map<String, String>
): SingleHttpClient {
    override fun requestSingle(request: HttpRequest): Single<HttpResponse> {
        val newRequest = request.copy(headers = request.headers + headers)
        return wrapped.requestSingle(newRequest)
    }
}