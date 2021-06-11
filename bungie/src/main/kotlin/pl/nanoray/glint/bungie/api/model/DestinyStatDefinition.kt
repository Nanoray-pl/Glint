package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.Serializable

@Serializable
data class DestinyStatDefinition(
    val displayProperties: DestinyDisplayPropertiesDefinition,
    val statCategory: DestinyStatCategory
)