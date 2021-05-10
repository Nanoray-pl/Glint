package pl.nanoray.glint.owner

import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container

class OwnerPlugin(
		container: Container
): ContainerEnabledPlugin(container) {
	init {
		register<WritableOwnerManager> { OwnerManagerImpl(it) }
	}
}