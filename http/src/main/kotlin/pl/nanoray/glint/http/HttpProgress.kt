package pl.nanoray.glint.http

sealed class HttpProgress {
	data class Indeterminate(
		val processedByteCount: Int = 0
	): HttpProgress()

	data class Determinate(
		val processedByteCount: Int,
		val expectedByteCount: Int
	): HttpProgress()
}