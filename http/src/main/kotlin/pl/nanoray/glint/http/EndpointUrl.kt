package pl.nanoray.glint.http

import io.mikael.urlbuilder.UrlBuilder
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer
import java.net.URL

data class EndpointUrl<PathParameters, QueryParameters>(
	val baseUrl: URL
) {
	class UnusuedPathParametersException(
		val parameters: Set<String>
	): Exception("Unusued parameters: $parameters")

	companion object {
		@PublishedApi internal val format = Properties.Default
		private const val encodedLeftCurlyBrace = "%7B" // encoded "{"
		private const val encodedRightCurlyBrace = "%7D" // encoded "}"

		@PublishedApi
		internal inline fun <reified T> createUrlWithQueryParameters(baseUrl: URL, queryParameters: T): URL {
			val map = format.encodeToStringMap(serializer(), queryParameters)
			var builder = UrlBuilder.fromUri(baseUrl.toURI())
			map.entries.forEach { (key, value) -> builder = builder.addParameter(key, value) }
			return builder.toUrl()
		}

		@PublishedApi
		internal inline fun <reified T> createUrlWithPathParameters(baseUrl: URL, pathParameters: T, ignoreUnusedPathParameters: Boolean = false): URL {
			val map = format.encodeToStringMap(serializer(), pathParameters)
			var builder = UrlBuilder.fromUri(baseUrl.toURI())
			val unusedParameters = mutableSetOf<String>()
			map.entries.forEach { (key, value) ->
				val path = builder.path
				val newPath = path.replace("{$key}", value)
				if (newPath == path)
					unusedParameters.add(key)
				builder = builder.withPath(newPath)
			}
			if (!ignoreUnusedPathParameters && unusedParameters.isNotEmpty())
				throw UnusuedPathParametersException(unusedParameters)
			return builder.toUrl()
		}
	}
}

fun EndpointUrl<Void, Void>.create(): URL {
	return baseUrl
}

inline fun <reified QueryParameters> EndpointUrl<Void, QueryParameters>.create(
	queryParameters: QueryParameters
): URL {
	return EndpointUrl.createUrlWithQueryParameters(baseUrl, queryParameters)
}

inline fun <reified PathParameters> EndpointUrl<PathParameters, Void>.create(
	pathParameters: PathParameters,
	ignoreUnusedPathParameters: Boolean = false
): URL {
	return EndpointUrl.createUrlWithPathParameters(baseUrl, pathParameters, ignoreUnusedPathParameters)
}

inline fun <reified PathParameters, reified QueryParameters> EndpointUrl<PathParameters, QueryParameters>.create(
	pathParameters: PathParameters,
	queryParameters: QueryParameters,
	ignoreUnusedPathParameters: Boolean = false
): URL {
	return EndpointUrl.createUrlWithQueryParameters(EndpointUrl.createUrlWithPathParameters(baseUrl, pathParameters, ignoreUnusedPathParameters), queryParameters)
}