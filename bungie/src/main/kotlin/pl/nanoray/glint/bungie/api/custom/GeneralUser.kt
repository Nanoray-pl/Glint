package pl.nanoray.glint.bungie.api.custom

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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