package pl.nanoray.glint.http

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlin.properties.Delegates
import kotlin.random.Random

class ErrorSimulationContext(
    errorOccurence: Occurence
) {
    sealed class Occurence {
        object Never: Occurence()
        object Always: Occurence()

        data class OneIn(
            val cycleLength: Int
        ): Occurence()

        data class Chance(
            val chance: Double,
            val random: Random
        ): Occurence()
    }

    enum class Mode {
        InsteadOfRequest, ReplacingResponse
    }

    private var requestCounter = 0

    var errorOccurence: Occurence by Delegates.observable(errorOccurence) { _, _, newValue ->
        if (newValue is Occurence.OneIn)
            requestCounter = 0
    }

    fun updateCounterAndReturnIfShouldSucceed(): Boolean {
        when (val errorOccurence = errorOccurence) {
            Occurence.Never -> return true
            Occurence.Always -> return false
            is Occurence.Chance -> return errorOccurence.random.nextDouble() >= errorOccurence.chance
            is Occurence.OneIn -> {
                requestCounter = (requestCounter + 1) % errorOccurence.cycleLength
                return requestCounter != 0
            }
        }
    }
}

class ErrorSimulatingObservableHttpClient(
    private val wrapped: ObservableHttpClient,
    var context: ErrorSimulationContext,
    var mode: ErrorSimulationContext.Mode,
    var throwableFactory: () -> Throwable
): ObservableHttpClient {
    override fun requestObservable(request: HttpRequest): Observable<HttpTaskOutput<HttpResponse>> {
        return Observable.defer {
            if (context.updateCounterAndReturnIfShouldSucceed()) {
                return@defer wrapped.requestObservable(request)
            } else {
                when (mode) {
                    ErrorSimulationContext.Mode.InsteadOfRequest -> return@defer Observable.error(throwableFactory)
                    ErrorSimulationContext.Mode.ReplacingResponse -> return@defer wrapped.requestObservable(request).flatMap { Observable.error(throwableFactory) }
                }
            }
        }
    }
}

class ErrorSimulatingSingleHttpClient(
    private val wrapped: SingleHttpClient,
    var context: ErrorSimulationContext,
    var mode: ErrorSimulationContext.Mode,
    var throwableFactory: () -> Throwable
): SingleHttpClient {
    override fun requestSingle(request: HttpRequest): Single<HttpResponse> {
        return Single.defer {
            if (context.updateCounterAndReturnIfShouldSucceed()) {
                return@defer wrapped.requestSingle(request)
            } else {
                when (mode) {
                    ErrorSimulationContext.Mode.InsteadOfRequest -> return@defer Single.error(throwableFactory)
                    ErrorSimulationContext.Mode.ReplacingResponse -> return@defer wrapped.requestSingle(request).flatMap { Single.error(throwableFactory) }
                }
            }
        }
    }
}