package pl.nanoray.glint.bungie.api.service.http.model

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.MembershipType
import pl.nanoray.glint.bungie.api.model.custom.MembershipId

@Serializable
data class BungieMembershipPathParameters(
    val membershipType: MembershipType,
    val membershipId: MembershipId
)