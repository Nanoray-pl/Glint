package pl.nanoray.glint.jdaextensions

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import net.dv8tion.jda.api.requests.RestAction
import pl.shockah.unikorn.Ref
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun <T> RestAction<T>.asSingle(): Single<T> {
	val lock = ReentrantLock()
	val futureRef = Ref<CompletableFuture<T>?>(null)
	return Single.defer {
		val future: CompletableFuture<T>
		lock.withLock {
			future = submit()
			futureRef.value = future
		}
		return@defer Single.fromFuture(future)
	}.doFinally {
		lock.withLock {
			futureRef.value?.cancel(true)
		}
	}
}

fun RestAction<Unit>.asCompletable(): Completable {
	val lock = ReentrantLock()
	val futureRef = Ref<CompletableFuture<Unit>?>(null)
	return Completable.defer {
		val future: CompletableFuture<Unit>
		lock.withLock {
			future = submit()
			futureRef.value = future
		}
		return@defer Completable.fromFuture(future)
	}.doFinally {
		lock.withLock {
			futureRef.value?.cancel(true)
		}
	}
}