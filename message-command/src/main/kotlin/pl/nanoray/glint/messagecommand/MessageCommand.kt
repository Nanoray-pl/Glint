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
		@Target(AnnotationTarget.VALUE_PARAMETER)
		annotation class Named(
				val name: String = "",
				val shorthand: String = "",
				val description: String
		) {
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.VALUE_PARAMETER)
			annotation class Flag(
					val name: String = "",
					val shorthand: String = "",
					val description: String
			)
		}

		@Retention(AnnotationRetention.RUNTIME)
		@Target(AnnotationTarget.VALUE_PARAMETER)
		annotation class Positional(
				val name: String = "",
				val description: String
		) {
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
			annotation class Final(
					val name: String = "",
					val description: String
			)
		}
	}

	sealed class ParsedOption(
			val name: String,
			val description: String,
			val isOptional: Boolean
	) {
		open class Named(
				name: String,
				val shorthand: String? = null,
				description: String,
				isOptional: Boolean
		): ParsedOption(name, description, isOptional) {
			class Flag(
					name: String,
					shorthand: String? = null,
					description: String,
					isOptional: Boolean
			): Named(name, shorthand, description, isOptional)
		}

		open class Positional(
				name: String,
				description: String,
				isOptional: Boolean
		): ParsedOption(name, description, isOptional) {
			class Final(
					name: String,
					description: String,
					isOptional: Boolean
			): Positional(name, description, isOptional)
		}
	}
}