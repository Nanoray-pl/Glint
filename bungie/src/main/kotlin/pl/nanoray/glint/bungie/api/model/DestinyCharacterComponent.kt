package pl.nanoray.glint.bungie.api.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ApiRelativeUrl
import pl.nanoray.glint.bungie.api.model.custom.CharacterId
import pl.nanoray.glint.bungie.api.model.custom.ManifestId
import pl.nanoray.glint.bungie.api.model.custom.MembershipId

@Serializable
data class DestinyCharacterComponent(
    val membershipId: MembershipId,
    val membershipType: MembershipType,
    val characterId: CharacterId,
    val dateLastPlayed: Instant,
    val minutesPlayedThisSession: Int,
    val minutesPlayedTotal: Int,
    val light: Int,
    val stats: Map<ManifestId<DestinyStatDefinition>, Int>,
    val emblemPath: ApiRelativeUrl,
    val emblemBackgroundPath: ApiRelativeUrl
)