package pl.nanoray.glint.plugin

import pl.shockah.unikorn.dependency.*
import pl.shockah.unikorn.plugin.Plugin
import kotlin.reflect.KClass
import kotlin.reflect.KType

abstract class ContainerEnabledPlugin(
		@PublishedApi internal val container: Container
): Plugin {
	@PublishedApi internal val componentKeys = mutableSetOf<ComponentId<*, *>>()
	private val pluginComponentId: ComponentId<*, *>

	protected val resolver: Resolver
		get() = container

	init {
		@Suppress("UNCHECKED_CAST", "LeakingThis")
		pluginComponentId = container.register(this)
	}

	override fun onUnload() {
		super.onUnload()
		for (componentKey in componentKeys) {
			container.unregister(componentKey)
		}
		container.unregister(pluginComponentId)
	}

	protected fun <T: Any, Key> register(id: ComponentId<T, Key>, componentStorageFactory: ComponentStorageFactory = container.defaultComponentStorageFactory, factory: (Resolver) -> T): ComponentId<T, Key> {
		componentKeys.add(id)
		return container.register(id, componentStorageFactory, factory)
	}

	protected fun <T: Any, Key> register(id: ComponentId<T, Key>, component: T, componentStorageFactory: ComponentStorageFactory = container.defaultComponentStorageFactory): ComponentId<T, Key> {
		componentKeys.add(id)
		return container.register(id, componentStorageFactory) { component }
	}

	protected fun <T: Any> register(type: KType, klass: KClass<T>, componentStorageFactory: ComponentStorageFactory = container.defaultComponentStorageFactory, factory: (Resolver) -> T): ComponentId<T, Unit> {
		val id = container.register(type, klass, componentStorageFactory, factory)
		componentKeys.add(id)
		return id
	}

	protected fun <T: Any> register(type: KType, klass: KClass<T>, component: T, componentStorageFactory: ComponentStorageFactory = container.defaultComponentStorageFactory): ComponentId<T, Unit> {
		val id = container.register(type, klass, component, componentStorageFactory)
		componentKeys.add(id)
		return id
	}

	protected inline fun <reified T: Any> register(componentStorageFactory: ComponentStorageFactory = container.defaultComponentStorageFactory, noinline factory: (Resolver) -> T): ComponentId<T, Unit> {
		val id = container.register(componentStorageFactory, factory)
		componentKeys.add(id)
		return id
	}

	protected inline fun <reified T: Any> register(component: T, componentStorageFactory: ComponentStorageFactory = container.defaultComponentStorageFactory): ComponentId<T, Unit> {
		val id = container.register(component, componentStorageFactory)
		componentKeys.add(id)
		return id
	}
}