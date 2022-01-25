package pl.nanoray.glint.http.oauth

import fi.iki.elonen.NanoHTTPD
import pl.nanoray.glint.http.HttpResponse
import java.io.Closeable
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import kotlin.io.path.readBytes

interface OAuth2RedirectHandler {
	fun handleOAuth2Redirect(state: String, parameters: Map<String, String>): HttpResponse?
}

interface OAuth2RedirectManager: Closeable {
	fun addHandler(handler: OAuth2RedirectHandler)
	fun removeHandler(handler: OAuth2RedirectHandler)
}

internal class OAuth2RedirectManagerImpl(
	config: Config
): NanoHTTPD(config.redirectServer.port), OAuth2RedirectManager {
	private val handlers = mutableListOf<OAuth2RedirectHandler>()

	init {
		when (config.redirectServer.ssl) {
			null -> { }
			else -> {
				val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
				val password = config.redirectServer.ssl.keystore.password
				keyStore.load(config.redirectServer.ssl.keystore.path.readBytes().inputStream(), password.toCharArray())
				val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
				keyManagerFactory.init(keyStore, password.toCharArray())
				makeSecure(makeSSLSocketFactory(keyStore, keyManagerFactory), null)
			}
		}
		start()
	}

	override fun close() {
		stop()
	}

	override fun addHandler(handler: OAuth2RedirectHandler) {
		handlers.add(handler)
	}

	override fun removeHandler(handler: OAuth2RedirectHandler) {
		handlers.remove(handler)
	}

	override fun serve(session: IHTTPSession?): Response {
		if (session == null)
			return super.serve(null)

		val parameters = session.parameters.mapValues { it.value.last() }
		val state = parameters["state"] ?: return serveError("Missing `state` parameter.")

		for (handler in handlers) {
			val response = handler.handleOAuth2Redirect(state, parameters) ?: continue
			val mimeType = response.headers["Content-Type"] ?: "text/plain"
			val result = newFixedLengthResponse(Response.Status.lookup(response.statusCode), mimeType, response.data.inputStream(), response.data.size.toLong())
			for ((headerKey, headerValue) in response.headers) {
				if (headerKey == "Content-Type")
					continue
				result.addHeader(headerKey, headerValue)
			}
			return result
		}

		return serveError("Cannot handle OAuth redirect.")
	}

	private fun serveError(message: String): Response {
		return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", message)
	}
}