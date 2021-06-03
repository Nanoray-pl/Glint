package pl.nanoray.glint.bungie.api.custom

import kotlinx.serialization.Serializable

@Serializable
data class ComponentResult<Data>(
    val data: Data
)