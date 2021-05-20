package pl.nanoray.glint.http

import java.util.*

class MultipartHttpBodyEncoder<A, B>(
		private val encoder1: Encoder<A>,
		private val encoder2: Encoder<B>
): HttpBodyEncoder<Pair<MultipartHttpBodyEncoder.Part<A>, MultipartHttpBodyEncoder.Part<B>>> {
	class Encoder<T>(
			val instance: HttpBodyEncoder<T>,
			val name: String
	)

	data class Part<T>(
			val body: T,
			val fileName: String? = null
	)

	private fun String.quoteEscaped(): String {
		return replace("\\", "\\\\").replace("\"", "\\\"")
	}

	private fun <T> append(part: Part<T>, target: MutableList<ByteArray>, encoder: Encoder<T>) {
		val contentDispositionValues = mutableListOf("form-data", "name=\"${encoder.name.quoteEscaped()}\"")
		if (part.fileName != null)
			contentDispositionValues += "filename=\"${part.fileName.quoteEscaped()}\""
		target += "Content-Disposition: ${contentDispositionValues.joinToString("; ")}\r\n".toByteArray()

		val encoded = encoder.instance.encodeHttpBody(part.body)
		target += "Content-Type: ${encoded.contentType}\r\n".toByteArray()
		target += "\r\n".toByteArray()
		target += encoded.data
	}

	private fun <T> append(part: Part<T>, target: MutableList<ByteArray>, encoder: Encoder<T>, boundary: String) {
		target += "--$boundary\r\n".toByteArray()
		append(part, target, encoder)
		target += "\r\n".toByteArray()
	}

	override fun encodeHttpBody(input: Pair<Part<A>, Part<B>>): HttpBodyEncoder.Result {
		val boundary = UUID.randomUUID().toString()
		val byteParts = mutableListOf<ByteArray>()

		append(input.first, byteParts, encoder1, boundary)
		append(input.second, byteParts, encoder2, boundary)
		byteParts += "--${boundary}--".toByteArray()

		val body: ByteArray = byteParts.fold(ByteArray(0)) { current, element -> current + element }
		return HttpBodyEncoder.Result("multipart/form-data; boundary=$boundary", body)
	}
}