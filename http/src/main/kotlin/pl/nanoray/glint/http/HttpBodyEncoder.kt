package pl.nanoray.glint.http

interface HttpBodyEncoder<Input> {
	data class Result(
		val contentType: String,
		val data: ByteArray
	) {
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Result

			if (contentType != other.contentType) return false
			if (!data.contentEquals(other.data)) return false

			return true
		}

		override fun hashCode(): Int {
			var result = contentType.hashCode()
			result = 31 * result + data.contentHashCode()
			return result
		}
	}

	fun encodeHttpBody(input: Input): Result
}