package pl.nanoray.glint.http

import java.net.URL

interface HttpRequestBuilder {
	fun buildRequest(method: HttpRequest.Method, url: URL, urlParameters: Map<String, Any> = emptyMap(), headers: Map<String, String> = emptyMap()): HttpRequest
	fun <T> buildRequest(method: HttpRequest.Method, url: URL, urlParameters: Map<String, Any> = emptyMap(), headers: Map<String, String> = emptyMap(), body: T, bodyEncoder: HttpBodyEncoder<T>): HttpRequest
}