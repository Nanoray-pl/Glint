package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.Serializable

@Serializable
data class UserMembershipData(
    val destinyMemberships: List<GroupUserInfoCard>,
    val bungieNetUser: GeneralUser
)