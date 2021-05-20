package pl.nanoray.glint.http

object RawHttpBodyEncoder: HttpBodyEncoder<HttpBodyEncoder.Result> {
	override fun encodeHttpBody(input: HttpBodyEncoder.Result): HttpBodyEncoder.Result {
		return input
	}
}