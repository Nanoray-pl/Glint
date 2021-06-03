package pl.nanoray.glint.bungie.api

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.custom.ManifestId

@Serializable
data class DestinyStatDefinition(
    val displayProperties: DestinyDisplayPropertiesDefinition,
    val statCategory: ManifestId<DestinyStatCategory>
)