package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.entities.Message
import kotlin.reflect.KClass

abstract class MessageCommand<Options: Any>(
		val optionsKlass: KClass<Options>
) {
	abstract val name: String
	abstract val description: String
	open val additionalDescription: String? = null
	open val subcommands: List<MessageCommand<*>> = emptyList()

	abstract fun handleCommand(message: Message, options: Options)

	object Option {
		@Retention(AnnotationRetention.RUNTIME)
		@Target(AnnotationTarget.VALUE_PARAMETER)
		annotation class Flag(
				val name: String = "",
				val shorthand: String = "",
				val description: String
		)

		@Retention(AnnotationRetention.RUNTIME)
		@Target(AnnotationTarget.VALUE_PARAMETER)
		annotation class Named(
				val name: String = "",
				val shorthand: String = "",
				val description: String
		)

		@Retention(AnnotationRetention.RUNTIME)
		@Target(AnnotationTarget.VALUE_PARAMETER)
		annotation class Positional(
				val name: String = "",
				val description: String
		)

		@Retention(AnnotationRetention.RUNTIME)
		@Target(AnnotationTarget.VALUE_PARAMETER)
		annotation class Final(
				val name: String = "",
				val description: String
		)
	}
}