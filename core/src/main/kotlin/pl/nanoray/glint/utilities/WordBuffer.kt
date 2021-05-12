package pl.nanoray.glint.utilities

import kotlin.properties.Delegates

class WordBuffer(
		val words: List<String>,
		val whitespace: List<String>
) {
	companion object {
		operator fun invoke(text: String, whitespacePredicate: (Int) -> Boolean = { Character.isWhitespace(it) }): WordBuffer {
			val words = mutableListOf<String>()
			val whitespace = mutableListOf<String>()

			var isMatchingWhitespace = false
			val builder = StringBuilder()
			for (codePoint in text.trim().codePoints()) {
				val isWhitespace = whitespacePredicate(codePoint)
				if (isWhitespace != isMatchingWhitespace) {
					(if (isMatchingWhitespace) whitespace else words).add(builder.toString())
					builder.clear()
					isMatchingWhitespace = !isMatchingWhitespace
				}
				builder.appendCodePoint(codePoint)
			}
			if (builder.isNotEmpty())
				(if (isMatchingWhitespace) whitespace else words).add(builder.toString())
			return WordBuffer(words, whitespace)
		}
	}

	var pointer by Delegates.vetoable(0) { _, _, new ->
		return@vetoable new >= 0 && new <= words.size
	}

	val startPointer: Int
		get() = 0

	val endPointer: Int
		get() = words.size

	init {
		require((words.isEmpty() && whitespace.isEmpty()) || words.size == whitespace.size + 1)
	}

	fun peekNextWord(): String? {
		if (pointer >= words.size)
			return null
		return words[pointer]
	}

	fun readNextWord(): String? {
		val result = peekNextWord()
		pointer++
		return result
	}

	fun peekWhole(): String? {
		if (pointer >= words.size)
			return null
		return buildString {
			for (i in pointer until words.size) {
				append(words[i])
				if (i != words.size - 1)
					append(whitespace[i])
			}
		}
	}

	fun readWhole(): String? {
		val result = peekWhole()
		pointer = words.size
		return result
	}
}