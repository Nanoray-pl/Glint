package pl.nanoray.glint.http

import io.mikael.urlbuilder.UrlBuilder
import java.net.URL

interface HttpRequestBuilder {
	fun buildRequest(method: HttpRequest.Method, url: URL, urlParameters: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): HttpRequest
	fun <T> buildRequest(method: HttpRequest.Method, url: URL, urlParameters: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap(), body: T, bodyEncoder: HttpBodyEncoder<T>): HttpRequest
}

object DefaultHttpRequestBuilder: HttpRequestBuilder {
	override fun buildRequest(
		method: HttpRequest.Method,
		url: URL,
		urlParameters: Map<String, String>,
		headers: Map<String, String>
	): HttpRequest {
		return HttpRequest(
			method,
			UrlBuilder.fromUrl(url).let {
				var result = it
				urlParameters.forEach { result = result.addParameter(it.key, it.value) }
				return@let result
			}.toUrl(),
			headers,
			ByteArray(0)
		)
	}

	override fun <T> buildRequest(
		method: HttpRequest.Method,
		url: URL,
		urlParameters: Map<String, String>,
		headers: Map<String, String>,
		body: T,
		bodyEncoder: HttpBodyEncoder<T>
	): HttpRequest {
		val encodedBody = bodyEncoder.encodeHttpBody(body)
		return HttpRequest(
			method,
			UrlBuilder.fromUrl(url).let {
				var result = it
				urlParameters.forEach { result = result.addParameter(it.key, it.value) }
				return@let result
			}.toUrl(),
			headers + listOf("Content-Type" to encodedBody.contentType).toMap(),
			encodedBody.data
		)
	}
}