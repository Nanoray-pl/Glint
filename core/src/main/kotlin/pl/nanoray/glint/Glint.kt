package pl.nanoray.glint

import com.xenomachina.argparser.ArgParser
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import pl.nanoray.glint.logic.CommandDataParser
import pl.nanoray.glint.logic.CommandDataParserImpl
import pl.nanoray.glint.plugin.PathPluginInfoProvider
import pl.nanoray.glint.plugin.PathPluginLoaderFactory
import pl.shockah.unikorn.dependency.*
import pl.shockah.unikorn.plugin.InstancePluginConstructorParameterHandler
import pl.shockah.unikorn.plugin.Plugin
import pl.shockah.unikorn.plugin.PluginConstructorParameterHandler
import pl.shockah.unikorn.plugin.PluginManager
import pl.shockah.unikorn.plugin.impl.SerialPluginManager
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
	private val pluginManager: PluginManager by container.inject()

	val resolver: Resolver
		get() = container

	init {
		container.resolve<JDA>()
		pluginManager.loadAllPlugins()
	}

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
		register<CommandDataParser> { CommandDataParserImpl(it) }
		register<PluginManager> {
			SerialPluginManager(
					infoProvider = PathPluginInfoProvider(arguments.pluginPath),
					loaderFactory = PathPluginLoaderFactory(),
					parameterHandlers = listOf(
							InstancePluginConstructorParameterHandler(container),
							object: PluginConstructorParameterHandler {
								override fun handleConstructorParameter(constructor: KFunction<Plugin>, parameter: KParameter): Any {
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