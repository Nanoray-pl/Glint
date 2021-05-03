package pl.nanoray.glint

import com.xenomachina.argparser.ArgParser
import net.dv8tion.jda.api.JDABuilder
import pl.shockah.unikorn.dependency.*
import pl.shockah.unikorn.plugin.Plugin
import pl.shockah.unikorn.plugin.PluginConstructorParameterHandler
import pl.shockah.unikorn.plugin.PluginInfo
import pl.shockah.unikorn.plugin.PluginManager
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

class Glint(
		val arguments: AppArguments
) {
	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Glint(ArgParser(args).parseInto(::ParsedAppArguments))
		}
	}

	private val container by lazy { Container().apply { configure() } }

	val resolver: Resolver
		get() = container

	private fun Container.configure() {
		configureConfiguration()
		configureBusinessLogic()
	}

	private fun Container.configureConfiguration() {
		register<ConfigManager> { ConfigManagerImpl(arguments.configPath) }
		register { it.resolve<ConfigManager>().getConfig(CoreConfig::class) ?: throw IllegalArgumentException("Cannot parse CoreConfig.") }
	}

	private fun Container.configureBusinessLogic() {
		register { JDABuilder.createDefault(it.resolve<CoreConfig>().token).build() }
		register {
			PluginManager(
					infoProvider = PluginInfo.Provider.Default(arguments.pluginPath.toFile()),
					additionalPluginConstructorParameterHandlers = listOf(
							object: PluginConstructorParameterHandler {
								override fun handleConstructorParameter(pluginInfo: PluginInfo, constructor: KFunction<Plugin>, parameter: KParameter): Any {
									if (parameter.type.classifier == Resolver::class || parameter.type.classifier == Container::class) {
										return container
									}
									throw PluginConstructorParameterHandler.UnhandledParameter()
								}
							},
							object: PluginConstructorParameterHandler {
								override fun handleConstructorParameter(pluginInfo: PluginInfo, constructor: KFunction<Plugin>, parameter: KParameter): Any {
									try {
										return it.resolve(parameter.type, parameter.type.classifier as KClass<*>)
									} catch (_: MissingComponentException) {
										throw PluginConstructorParameterHandler.UnhandledParameter()
									}
								}
							}
					)
			)
		}
		register<DurationParser> { DefaultDurationParser() }
	}
}