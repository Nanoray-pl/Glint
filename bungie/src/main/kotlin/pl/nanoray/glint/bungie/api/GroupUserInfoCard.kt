package pl.nanoray.glint.bungie.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.custom.ApiRelativeUrl
import pl.nanoray.glint.bungie.api.custom.MembershipId
import pl.nanoray.glint.bungie.api.custom.PlatformType

@Serializable
data class GroupUserInfoCard(
    @SerialName("LastSeenDisplayName") val lastSeenDisplayName: String,
    @SerialName("LastSeenDisplayNameType") val lastSeenDisplayNameType: PlatformType,
    val iconPath: ApiRelativeUrl,
    val applicableMembershipTypes: Set<MembershipType>,
    val isPublic: Boolean,
    val membershipType: MembershipType,
    val membershipId: MembershipId,
    val displayName: String
)