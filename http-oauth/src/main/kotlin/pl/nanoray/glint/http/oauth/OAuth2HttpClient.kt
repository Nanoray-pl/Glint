package pl.nanoray.glint.http.oauth

import com.github.scribejava.core.oauth.OAuth20Service
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.datetime.Clock
import pl.nanoray.glint.http.*
import pl.nanoray.glint.store.Store
import java.io.Closeable
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.toDuration

abstract class OAuth2HttpClient<UserId>(
	protected val wrapped: SingleHttpClient,
	private val userId: UserId,
	private val oAuth2RedirectManager: OAuth2RedirectManager,
	private val service: OAuth20Service,
	private val tokenStore: Store<OAuth2Token?>
): Closeable {
	class UnauthorizedException: Exception()

	private data class AuthorizationRequest<UserId>(
		val userId: UserId,
		val timeoutMark: TimeMark,
		val id: UUID
	)

	protected val lock = ReentrantLock()
	private var isHandlerAdded = false
	private val handler = Handler()
	private val authorizationRequests = mutableListOf<AuthorizationRequest<UserId>>()

	fun startAuthorization(timeout: Duration = 15.toDuration(TimeUnit.MINUTES)): URL {
		return lock.withLock {
			val timeoutMark = TimeSource.Monotonic.markNow() + timeout
			val id = UUID.randomUUID()
			authorizationRequests.add(AuthorizationRequest(userId, timeoutMark, id))
			if (!isHandlerAdded) {
				oAuth2RedirectManager.addHandler(handler)
				isHandlerAdded = true
			}
			return@withLock URL(service.getAuthorizationUrl("$id"))
		}
	}

	override fun close() {
		lock.withLock {
			if (isHandlerAdded) {
				oAuth2RedirectManager.removeHandler(handler)
				isHandlerAdded = false
			}
		}
	}

	protected fun refreshIfNeededAndModifyRequest(request: HttpRequest): Single<HttpRequest> {
		return Single.defer {
			val token = lock.withLock { tokenStore.value } ?: return@defer Single.error(UnauthorizedException())
			if (token.expiryTime >= Clock.System.now()) {
				val refreshToken = token.refreshToken ?: return@defer Single.error(UnauthorizedException())
				return@defer Single.fromFuture(service.refreshAccessTokenAsync(refreshToken))
					.map { apiToken -> return@map lock.withLock {
						val newToken = OAuth2Token(apiToken.accessToken, apiToken.refreshToken, Clock.System.now() + apiToken.expiresIn.toDuration(TimeUnit.SECONDS))
						tokenStore.value = newToken
						return@withLock newToken
					} }
					.map { getAuthorizedRequest(it, request) }
			} else {
				return@defer Single.just(getAuthorizedRequest(token, request))
			}
		}
	}

	private fun getAuthorizedRequest(token: OAuth2Token, request: HttpRequest): HttpRequest {
		return request.copy(headers = request.headers + mapOf("Authorization" to "Bearer ${token.accessToken}"))
	}

	private inner class Handler: OAuth2RedirectHandler {
		override fun handleOAuth2Redirect(state: String, parameters: Map<String, String>): HttpResponse? {
			return lock.withLock {
				val iterator = authorizationRequests.iterator()
				while (iterator.hasNext()) {
					val request = iterator.next()
					if ("${request.id}" != state)
						continue
					val code = parameters["code"] ?: continue
					iterator.remove()
					Single.fromFuture(service.getAccessTokenAsync(code))
						.subscribe { token -> lock.withLock {
							tokenStore.value = OAuth2Token(token.accessToken, token.refreshToken, Clock.System.now() + token.expiresIn.toDuration(TimeUnit.SECONDS))
						} }
					return@withLock HttpResponse(
						200,
						mapOf(
							"Content-Type" to "text/plain"
						),
						"Authorized".toByteArray()
					)
				}
				return@withLock null
			}
		}
	}
}

class OAuth2SingleHttpClient<UserId>(
	wrapped: SingleHttpClient,
	userId: UserId,
	oAuth2RedirectManager: OAuth2RedirectManager,
	service: OAuth20Service,
	tokenStore: Store<OAuth2Token?>
): OAuth2HttpClient<UserId>(wrapped, userId, oAuth2RedirectManager, service, tokenStore), SingleHttpClient {
	override fun requestSingle(request: HttpRequest): Single<HttpResponse> {
		return refreshIfNeededAndModifyRequest(request)
			.flatMap { wrapped.requestSingle(it) }
	}
}

class OAuth2ObservableHttpClient<UserId>(
	wrapped: ObservableHttpClient,
	userId: UserId,
	oAuth2RedirectManager: OAuth2RedirectManager,
	service: OAuth20Service,
	tokenStore: Store<OAuth2Token?>
): OAuth2HttpClient<UserId>(wrapped, userId, oAuth2RedirectManager, service, tokenStore), ObservableHttpClient {
	private val observableWrapped: ObservableHttpClient
		get() = wrapped as ObservableHttpClient

	override fun requestObservable(request: HttpRequest): Observable<HttpTaskOutput<HttpResponse>> {
		return refreshIfNeededAndModifyRequest(request)
			.flatMapObservable { observableWrapped.requestObservable(it) }
	}
}