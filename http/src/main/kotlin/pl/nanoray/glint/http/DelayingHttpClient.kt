package pl.nanoray.glint.http

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit

fun ObservableHttpClient.delaying(time: Long, unit: TimeUnit): ObservableHttpClient {
    return DelayingObservableHttpClient(this, time, unit)
}

fun SingleHttpClient.delaying(time: Long, unit: TimeUnit): SingleHttpClient {
    return DelayingSingleHttpClient(this, time, unit)
}

class DelayingObservableHttpClient(
    private val wrapped: ObservableHttpClient,
    val time: Long,
    val unit: TimeUnit
): ObservableHttpClient {
    override fun requestObservable(request: HttpRequest): Observable<HttpTaskOutput<HttpResponse>> {
        return wrapped.requestObservable(request)
            .delaySubscription(time, unit)
    }
}

class DelayingSingleHttpClient(
    private val wrapped: SingleHttpClient,
    val time: Long,
    val unit: TimeUnit
): SingleHttpClient {
    override fun requestSingle(request: HttpRequest): Single<HttpResponse> {
        return wrapped.requestSingle(request)
            .delaySubscription(time, unit)
    }
}