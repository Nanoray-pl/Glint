package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ApiRelativeUrl
import pl.nanoray.glint.bungie.api.model.custom.MembershipId
import pl.nanoray.glint.bungie.api.model.custom.PlatformType

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