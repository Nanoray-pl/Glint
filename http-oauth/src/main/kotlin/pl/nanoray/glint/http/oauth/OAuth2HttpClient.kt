package pl.nanoray.glint.http.oauth

import com.github.scribejava.core.oauth.OAuth20Service
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.datetime.Clock
import pl.nanoray.glint.http.*
import pl.nanoray.glint.store.Store
import java.io.Closeable
import java.net.URL
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.*

abstract class OAuth2HttpClient<UserId, Token: OAuth2Token>(
	protected val wrapped: SingleHttpClient,
	private val userId: UserId,
	private val oAuth2RedirectManager: OAuth2RedirectManager,
	private val service: OAuth20Service,
	private val tokenParser: TokenParser<Token>,
	private val tokenStore: Store<Token?>
): Closeable {
	class UnauthorizedException: Exception()
	class CannotRefreshException: Exception()

	private data class AuthorizationRequest<UserId>(
		val userId: UserId,
		val timeoutMark: TimeMark, // TODO: handle timeout
		val id: UUID
	)

	val token: Token?
		get() = lock.withLock { tokenStore.value }

	protected val lock = ReentrantLock()
	private var isHandlerAdded = false
	private val handler = Handler()
	private val authorizationRequests = mutableListOf<AuthorizationRequest<UserId>>()

	fun startAuthorization(timeout: Duration = 15.toDuration(DurationUnit.MINUTES)): URL {
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

	fun refreshTokens(): Completable {
		return Completable.defer {
			val token = lock.withLock { tokenStore.value } ?: return@defer Completable.error(UnauthorizedException())
			val refreshToken = token.refreshToken ?: return@defer Completable.error(CannotRefreshException())
			if (refreshToken.expiryTime != null && refreshToken.expiryTime >= Clock.System.now())
				return@defer Completable.error(CannotRefreshException())
			return@defer Single.fromFuture(service.refreshAccessTokenAsync(refreshToken.token))
				.map { apiToken -> return@map lock.withLock {
					val newToken = tokenParser.parseToken(apiToken)
					tokenStore.value = newToken
					return@withLock newToken
				} }
				.ignoreElement()
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
			val expiryTime = token.accessToken.expiryTime
			if (expiryTime != null && expiryTime >= Clock.System.now()) {
				val refreshToken = token.refreshToken ?: return@defer Single.error(UnauthorizedException())
				if (refreshToken.expiryTime != null && refreshToken.expiryTime >= Clock.System.now())
					return@defer Single.error(CannotRefreshException())
				return@defer Single.fromFuture(service.refreshAccessTokenAsync(refreshToken.token))
					.map { apiToken -> return@map lock.withLock {
						val newToken = tokenParser.parseToken(apiToken)
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
							tokenStore.value = tokenParser.parseToken(token)
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

class OAuth2SingleHttpClient<UserId, Token: OAuth2Token>(
	wrapped: SingleHttpClient,
	userId: UserId,
	oAuth2RedirectManager: OAuth2RedirectManager,
	service: OAuth20Service,
	tokenParser: TokenParser<Token>,
	tokenStore: Store<Token?>
): OAuth2HttpClient<UserId, Token>(wrapped, userId, oAuth2RedirectManager, service, tokenParser, tokenStore), SingleHttpClient {
	override fun requestSingle(request: HttpRequest): Single<HttpResponse> {
		return refreshIfNeededAndModifyRequest(request)
			.flatMap { wrapped.requestSingle(it) }
	}
}

class OAuth2ObservableHttpClient<UserId, Token: OAuth2Token>(
	wrapped: ObservableHttpClient,
	userId: UserId,
	oAuth2RedirectManager: OAuth2RedirectManager,
	service: OAuth20Service,
	tokenParser: TokenParser<Token>,
	tokenStore: Store<Token?>
): OAuth2HttpClient<UserId, Token>(wrapped, userId, oAuth2RedirectManager, service, tokenParser, tokenStore), ObservableHttpClient {
	private val observableWrapped: ObservableHttpClient
		get() = wrapped as ObservableHttpClient

	override fun requestObservable(request: HttpRequest): Observable<HttpTaskOutput<HttpResponse>> {
		return refreshIfNeededAndModifyRequest(request)
			.flatMapObservable { observableWrapped.requestObservable(it) }
	}
}