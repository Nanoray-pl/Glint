package pl.nanoray.glint.bungie.api

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.custom.ApiRelativeUrl

@Serializable
data class DestinyDisplayPropertiesDefinition(
    val name: String,
    val description: String,
    val icon: ApiRelativeUrl? = null
)