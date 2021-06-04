package pl.nanoray.glint.bungie.api.model

import kotlinx.serialization.Serializable
import pl.nanoray.glint.bungie.api.model.custom.ApiRelativeUrl

@Serializable
data class DestinyDisplayPropertiesDefinition(
    val name: String,
    val description: String,
    val icon: ApiRelativeUrl? = null
)