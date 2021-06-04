package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ManifestId

@Serializable
data class DestinyStatDefinition(
    val displayProperties: DestinyDisplayPropertiesDefinition,
    val statCategory: ManifestId<DestinyStatCategory>
)