package pl.nanoray.glint.owner

import net.dv8tion.jda.api.entities.User
import pl.nanoray.glint.jdaextensions.UserIdentifier
import pl.nanoray.glint.jdaextensions.identifier

interface OwnerManager {
	fun isOwner(user: UserIdentifier): Boolean
}

interface WritableOwnerManager: OwnerManager {
	fun setIsOwner(user: UserIdentifier, isOwner: Boolean = true)
}

fun OwnerManager.isOwner(user: User): Boolean {
	return isOwner(user.identifier)
}

fun WritableOwnerManager.setIsOwner(user: User, isOwner: Boolean = true) {
	setIsOwner(user.identifier, isOwner)
}