package pl.nanoray.glint.http

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlin.properties.Delegates
import kotlin.random.Random

class ErrorSimulationContext(
    errorOccurrence: Occurrence
) {
    sealed class Occurrence {
        object Never: Occurrence()
        object Always: Occurrence()

        data class OneIn(
            val cycleLength: Int
        ): Occurrence()

        data class Chance(
            val chance: Double,
            val random: Random
        ): Occurrence()
    }

    enum class Mode {
        InsteadOfRequest, ReplacingResponse
    }

    private var requestCounter = 0

    var errorOccurrence: Occurrence by Delegates.observable(errorOccurrence) { _, _, newValue ->
        if (newValue is Occurrence.OneIn)
            requestCounter = 0
    }

    fun updateCounterAndReturnIfShouldSucceed(): Boolean {
        when (val errorOccurrence = errorOccurrence) {
            Occurrence.Never -> return true
            Occurrence.Always -> return false
            is Occurrence.Chance -> return errorOccurrence.random.nextDouble() >= errorOccurrence.chance
            is Occurrence.OneIn -> {
                requestCounter = (requestCounter + 1) % errorOccurrence.cycleLength
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