package pl.nanoray.glint.owner

import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.register

class OwnerPlugin(
		container: Container
): ContainerEnabledPlugin(container) {
	init {
		container.register<OwnerManager> { OwnerManagerImpl(it) }
	}
}