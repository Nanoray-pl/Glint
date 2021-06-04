package pl.nanoray.glint.bungie.api.service

import io.reactivex.rxjava3.core.Single
import pl.nanoray.glint.bungie.api.model.DestinyProfileResponse
import pl.nanoray.glint.bungie.api.model.MembershipType
import pl.nanoray.glint.bungie.api.model.UserMembershipData
import pl.nanoray.glint.bungie.api.model.custom.MembershipId

interface BungieUserService {
    fun getMembershipsForCurrentUser(): Single<UserMembershipData>
    fun getProfile(membershipType: MembershipType, membershipId: MembershipId, components: Collection<DestinyProfileResponse.Component>): Single<DestinyProfileResponse>
}