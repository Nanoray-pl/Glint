package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ManifestId

@Serializable
data class DestinyStatDisplayDefinition(
    val statHash: ManifestId<DestinyStatDefinition>,
    val maximumValue: Int,
    val displayAsNumeric: Boolean
)