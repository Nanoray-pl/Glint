package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ManifestId

@Serializable
data class DestinyStatGroupDefinition(
    val hash: ManifestId<DestinyStatGroupDefinition>,
    val uiPosition: Int,
    val index: Int,
    val maximumValue: Int,
    val scaledStats: List<DestinyStatDisplayDefinition>
)