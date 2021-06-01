package pl.nanoray.glint.http.oauth

import com.github.scribejava.core.oauth.OAuth20Service
import kotlinx.datetime.toJavaInstant
import pl.nanoray.glint.http.ObservableHttpClient
import pl.nanoray.glint.http.SingleHttpClient
import pl.nanoray.glint.store.Store
import pl.nanoray.glint.store.forKey
import java.util.*
import kotlin.concurrent.schedule

interface OAuth2HttpClientManager<UserId, Token: OAuth2Token> {
	fun getHttpClient(userId: UserId): OAuth2HttpClient<UserId, Token>
}

interface OAuth2SingleHttpClientManager<UserId, Token: OAuth2Token>: OAuth2HttpClientManager<UserId, Token> {
	override fun getHttpClient(userId: UserId): OAuth2SingleHttpClient<UserId, Token>
}

interface OAuth2ObservableHttpClientManager<UserId, Token: OAuth2Token>: OAuth2HttpClientManager<UserId, Token> {
	override fun getHttpClient(userId: UserId): OAuth2ObservableHttpClient<UserId, Token>
}

class StoreOAuth2SingleHttpClientManager<UserId, Token: OAuth2Token>(
	private val wrappedClient: SingleHttpClient,
	private val store: Store<Map<UserId, Token>>,
	private val redirectManager: OAuth2RedirectManager,
	private val service: OAuth20Service,
	private val tokenParser: TokenParser<Token>
): OAuth2SingleHttpClientManager<UserId, Token> {
	private val timer = Timer()
	private val clients = mutableMapOf<UserId, OAuth2SingleHttpClient<UserId, Token>>().apply {
		for (userId in store.value.keys) {
			this[userId] = OAuth2SingleHttpClient(wrappedClient, userId, redirectManager, service, tokenParser, store.forKey(userId))
		}
	}

	init {
		scheduleNextClientRefresh()
	}

	private fun scheduleNextClientRefresh() {
		timer.purge()
		val (client, token, refreshTokenExpiryTime) = clients.entries
			.mapNotNull { (userId, client) -> store.value[userId]?.let { client to it } }
			.mapNotNull { (client, token) -> token.refreshToken?.expiryTime?.let { Triple(client, token, it) } }
			.minByOrNull { (_, _, refreshTokenExpiryTime) -> refreshTokenExpiryTime } ?: return
		val difference = refreshTokenExpiryTime - token.creationTime
		val halfTime = token.creationTime + difference / 2
		timer.schedule(Date.from(halfTime.toJavaInstant())) {
			client.refreshTokens()
				.doOnTerminate { scheduleNextClientRefresh() }
				.subscribe()
		}
	}

	override fun getHttpClient(userId: UserId): OAuth2SingleHttpClient<UserId, Token> {
		return clients.computeIfAbsent(userId) { OAuth2SingleHttpClient(wrappedClient, userId, redirectManager, service, tokenParser, store.forKey(userId)) }
	}
}

class StoreOAuth2ObservableHttpClientManager<UserId, Token: OAuth2Token>(
	private val wrappedClient: ObservableHttpClient,
	private val store: Store<Map<UserId, Token>>,
	private val redirectManager: OAuth2RedirectManager,
	private val service: OAuth20Service,
	private val tokenParser: TokenParser<Token>
): OAuth2ObservableHttpClientManager<UserId, Token> {
	private val timer = Timer()
	private val clients = mutableMapOf<UserId, OAuth2ObservableHttpClient<UserId, Token>>().apply {
		for (userId in store.value.keys) {
			this[userId] = OAuth2ObservableHttpClient(wrappedClient, userId, redirectManager, service, tokenParser, store.forKey(userId))
		}
	}

	init {
		scheduleNextClientRefresh()
	}

	private fun scheduleNextClientRefresh() {
		timer.purge()
		val (client, token, refreshTokenExpiryTime) = clients.entries
			.mapNotNull { (userId, client) -> store.value[userId]?.let { client to it } }
			.mapNotNull { (client, token) -> token.refreshToken?.expiryTime?.let { Triple(client, token, it) } }
			.minByOrNull { (_, _, refreshTokenExpiryTime) -> refreshTokenExpiryTime } ?: return
		val difference = refreshTokenExpiryTime - token.creationTime
		val halfTime = token.creationTime + difference / 2
		timer.schedule(Date.from(halfTime.toJavaInstant())) {
			client.refreshTokens()
				.doOnTerminate { scheduleNextClientRefresh() }
				.subscribe()
		}
	}

	override fun getHttpClient(userId: UserId): OAuth2ObservableHttpClient<UserId, Token> {
		return clients.computeIfAbsent(userId) { OAuth2ObservableHttpClient(wrappedClient, userId, redirectManager, service, tokenParser, store.forKey(userId)) }
	}
}