package pl.nanoray.glint.jdaextensions

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import net.dv8tion.jda.api.requests.RestAction

fun <T: Any> RestAction<T>.toSingle(): Single<T> {
	return Single.defer { Single.fromFuture(submit()) }
}

fun RestAction<Void>.toCompletable(): Completable {
	return Completable.defer { Completable.fromFuture(submit()) }
}

@JvmName("toCompletableUnit")
fun RestAction<Unit>.toCompletable(): Completable {
	return Completable.defer { Completable.fromFuture(submit()) }
}