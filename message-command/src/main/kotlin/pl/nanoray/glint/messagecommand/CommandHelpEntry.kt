package pl.nanoray.glint.messagecommand

data class CommandHelpEntry(
		val name: String,
		val description: String,
		val options: List<Option>
) {
	data class Option(
			val name: String,
			val shorthand: String? = null,
			val description: String,
			val isOptional: Boolean,
			val type: Type
	) {
		enum class Type {
			Flag, Named, Positional, Final
		}
	}
}