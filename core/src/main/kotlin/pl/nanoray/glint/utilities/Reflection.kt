package pl.nanoray.glint.utilities

import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf

inline fun <reified T: Any> createNullabilityTypeVariants(): Set<KType> {
	val source = typeOf<T>()
	return source.classifier?.let { setOf(
			it.createType(source.arguments, false, source.annotations),
			it.createType(source.arguments, true, source.annotations)
	) } ?: emptySet()
}