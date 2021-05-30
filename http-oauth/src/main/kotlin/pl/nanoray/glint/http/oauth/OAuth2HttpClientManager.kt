package pl.nanoray.glint.http.oauth

import com.github.scribejava.core.oauth.OAuth20Service
import pl.nanoray.glint.http.ObservableHttpClient
import pl.nanoray.glint.http.SingleHttpClient
import pl.nanoray.glint.store.Store
import pl.nanoray.glint.store.forKey

interface OAuth2HttpClientManager<UserId> {
	fun getHttpClient(userId: UserId): OAuth2HttpClient<UserId>
}

interface OAuth2SingleHttpClientManager<UserId>: OAuth2HttpClientManager<UserId> {
	override fun getHttpClient(userId: UserId): OAuth2SingleHttpClient<UserId>
}

interface OAuth2ObservableHttpClientManager<UserId>: OAuth2HttpClientManager<UserId> {
	override fun getHttpClient(userId: UserId): OAuth2ObservableHttpClient<UserId>
}

abstract class StoreOAuth2HttpClientManager<UserId>(
	protected val store: Store<Map<UserId, OAuth2Token>>,
	protected val redirectManager: OAuth2RedirectManager,
	protected val service: OAuth20Service
): OAuth2HttpClientManager<UserId>

class StoreOAuth2SingleHttpClientManager<UserId>(
	private val wrappedClient: SingleHttpClient,
	store: Store<Map<UserId, OAuth2Token>>,
	redirectManager: OAuth2RedirectManager,
	service: OAuth20Service
): StoreOAuth2HttpClientManager<UserId>(store, redirectManager, service), OAuth2SingleHttpClientManager<UserId> {
	val clients = mutableMapOf<UserId, OAuth2SingleHttpClient<UserId>>().apply {
		for (userId in store.value.keys) {
			this[userId] = OAuth2SingleHttpClient(wrappedClient, userId, redirectManager, service, store.forKey(userId))
		}
	}

	override fun getHttpClient(userId: UserId): OAuth2SingleHttpClient<UserId> {
		return clients.computeIfAbsent(userId) { OAuth2SingleHttpClient(wrappedClient, userId, redirectManager, service, store.forKey(userId)) }
	}
}

class StoreOAuth2ObservableHttpClientManager<UserId>(
	private val wrappedClient: ObservableHttpClient,
	store: Store<Map<UserId, OAuth2Token>>,
	redirectManager: OAuth2RedirectManager,
	service: OAuth20Service
): StoreOAuth2HttpClientManager<UserId>(store, redirectManager, service), OAuth2ObservableHttpClientManager<UserId> {
	val clients = mutableMapOf<UserId, OAuth2ObservableHttpClient<UserId>>().apply {
		for (userId in store.value.keys) {
			this[userId] = OAuth2ObservableHttpClient(wrappedClient, userId, redirectManager, service, store.forKey(userId))
		}
	}

	override fun getHttpClient(userId: UserId): OAuth2ObservableHttpClient<UserId> {
		return clients.computeIfAbsent(userId) { OAuth2ObservableHttpClient(wrappedClient, userId, redirectManager, service, store.forKey(userId)) }
	}
}