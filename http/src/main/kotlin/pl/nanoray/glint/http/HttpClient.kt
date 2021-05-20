package pl.nanoray.glint.http

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface SingleHttpClient {
	fun requestSingle(request: HttpRequest): Single<HttpResponse>

	fun requestCompletable(request: HttpRequest): Completable {
		return requestSingle(request).ignoreElement()
	}
}

interface ObservableHttpClient: SingleHttpClient {
	fun requestObservable(request: HttpRequest): Observable<HttpTaskOutput<HttpResponse>>

	override fun requestSingle(request: HttpRequest): Single<HttpResponse> {
		return requestObservable(request)
				.filter { it is HttpTaskOutput.Response<*> && it.output is HttpResponse }
				.map { (it as HttpTaskOutput.Response<*>).output as HttpResponse }
				.singleOrError()
	}
}