package pl.nanoray.glint.http

sealed class HttpTaskOutput<Output> {
	data class SendProgress<Output>(
		val progress: HttpProgress
	): HttpTaskOutput<Output>()

	data class ReceiveProgress<Output>(
		val progress: HttpProgress
	): HttpTaskOutput<Output>()

	data class Response<Output>(
		val output: Output
	): HttpTaskOutput<Output>()
}