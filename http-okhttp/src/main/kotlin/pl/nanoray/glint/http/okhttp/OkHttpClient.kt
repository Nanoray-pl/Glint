package pl.nanoray.glint.http.okhttp

import io.reactivex.rxjava3.core.Single
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import pl.nanoray.glint.http.HttpRequest
import pl.nanoray.glint.http.HttpResponse
import pl.nanoray.glint.http.SingleHttpClient
import java.io.IOException
import java.util.*
import okhttp3.OkHttpClient as Client

internal class OkHttpClient(
    private val client: Client
): SingleHttpClient {
    override fun requestSingle(request: HttpRequest): Single<HttpResponse> {
        val contentType = requireNotNull(request.headers["Content-Type"]) { "`Content-Type` is a required header." }
        val tag = UUID.randomUUID()
        val requestBuilder = Request.Builder().url(request.url).tag(tag)
        when (request.method) {
            HttpRequest.Method.HEAD -> {
                require(request.body.isEmpty()) { "Cannot send a body for a HEAD HTTP request." }
                requestBuilder.head()
            }
            HttpRequest.Method.GET -> {
                require(request.body.isEmpty()) { "Cannot send a body for a GET HTTP request." }
                requestBuilder.get()
            }
            HttpRequest.Method.POST -> requestBuilder.post(request.body.toRequestBody(contentType.toMediaTypeOrNull()))
            HttpRequest.Method.PUT -> requestBuilder.put(request.body.toRequestBody(contentType.toMediaTypeOrNull()))
            HttpRequest.Method.PATCH -> requestBuilder.patch(request.body.toRequestBody(contentType.toMediaTypeOrNull()))
            HttpRequest.Method.DELETE -> requestBuilder.delete(request.body.toRequestBody(contentType.toMediaTypeOrNull()))
        }
        for ((headerKey, headerValue) in request.headers) {
            requestBuilder.addHeader(headerKey, headerValue)
        }

        val clientRequest = requestBuilder.build()
        return Single.defer {
            Single.create { emitter ->
                emitter.setCancellable {
                    client.dispatcher.queuedCalls().firstOrNull { it.request().tag() == tag }?.cancel()
                    client.dispatcher.runningCalls().firstOrNull { it.request().tag() == tag }?.cancel()
                }

                client.newCall(clientRequest).enqueue(object: Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        emitter.onError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        emitter.onSuccess(HttpResponse(response.code, response.headers.toMap(), response.body?.bytes() ?: ByteArray(0)))
                    }
                })
            }
        }
    }
}