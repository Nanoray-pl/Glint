package pl.nanoray.glint.bungie.api.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ApiRelativeUrl
import pl.nanoray.glint.bungie.api.model.custom.MembershipId

@Serializable
data class GeneralUser(
    val membershipId: MembershipId,
    val uniqueName: String,
    val displayName: String,
    val firstAccess: Instant,
    val lastUpdate: Instant,
    val profilePicturePath: ApiRelativeUrl,
    val blizzardDisplayName: String? = null,
    val steamDisplayName: String? = null,
    val stadiaDisplayName: String? = null
)