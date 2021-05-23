package pl.nanoray.glint.http

import java.net.URL

data class HttpRequest(
	val method: Method,
	val url: URL,
	val headers: Map<String, String> = emptyMap(),
	val body: ByteArray
) {
	enum class Method {
		GET, PUT, POST, PATCH, DELETE, HEAD
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as HttpRequest

		if (method != other.method) return false
		if (url != other.url) return false
		if (headers != other.headers) return false
		if (!body.contentEquals(other.body)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = method.hashCode()
		result = 31 * result + url.hashCode()
		result = 31 * result + headers.hashCode()
		result = 31 * result + body.contentHashCode()
		return result
	}
}