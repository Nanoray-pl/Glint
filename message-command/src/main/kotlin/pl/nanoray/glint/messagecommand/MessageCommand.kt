package pl.nanoray.glint.messagecommand

import net.dv8tion.jda.api.entities.Message
import pl.nanoray.glint.command.CommandPredicate
import kotlin.reflect.KClass
import kotlin.reflect.KType

abstract class MessageCommand<Options>(
		val optionsType: KType,
		val optionsKlass: KClass<*>
) {
	constructor(optionsType: KType): this(optionsType, optionsType.classifier as? KClass<*> ?: throw IllegalArgumentException())

	abstract val name: String
	abstract val description: String
	open val additionalDescription: String? = null
	open val subcommands: List<MessageCommand<*>> = emptyList()
	open val predicates: List<CommandPredicate> = emptyList()

	abstract fun handleCommand(message: Message, options: Options)

	object Option {
		@Retention(AnnotationRetention.RUNTIME)
		@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
		annotation class Flag(
				val name: String = "",
				val shorthand: String = "",
				val description: String
		)

		@Retention(AnnotationRetention.RUNTIME)
		@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
		annotation class Named(
				val name: String = "",
				val shorthand: String = "",
				val description: String
		)

		@Retention(AnnotationRetention.RUNTIME)
		@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
		annotation class Positional(
				val name: String = "",
				val description: String
		)

		@Retention(AnnotationRetention.RUNTIME)
		@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
		annotation class Final(
				val name: String = "",
				val description: String
		)
	}
}