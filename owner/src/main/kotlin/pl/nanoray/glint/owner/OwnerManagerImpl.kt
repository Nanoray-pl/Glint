package pl.nanoray.glint.owner

import pl.nanoray.glint.ConfigManager
import pl.nanoray.glint.getConfig
import pl.nanoray.glint.jdaextensions.UserIdentifier
import pl.nanoray.glint.storeConfig
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class OwnerManagerImpl(
		resolver: Resolver
): WritableOwnerManager {
	private val configManager: ConfigManager by resolver.inject()

	private val lock = ReentrantReadWriteLock()
	private var ownersStorage: Set<UserIdentifier>? = null

	private var owners: Set<UserIdentifier>
		get() = lock.read {
			val owners = ownersStorage
			if (owners == null) {
				val config = configManager.getConfig() ?: Config(emptySet())
				ownersStorage = config.owners
				return@read config.owners
			} else {
				return@read owners
			}
		}
		set(value) = lock.write {
			ownersStorage = value
			configManager.storeConfig(Config(value))
		}

	override fun isOwner(user: UserIdentifier): Boolean {
		return owners.contains(user)
	}

	override fun setIsOwner(user: UserIdentifier, isOwner: Boolean) {
		if (isOwner)
			owners += user
		else
			owners -= user
	}
}