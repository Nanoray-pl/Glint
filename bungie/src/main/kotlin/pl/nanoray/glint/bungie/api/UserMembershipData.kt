package pl.nanoray.glint.bungie.api

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.custom.GeneralUser

@Serializable
data class UserMembershipData(
    val destinyMemberships: List<GroupUserInfoCard>,
    val bungieNetUser: GeneralUser
)