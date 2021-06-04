package pl.nanoray.glint.bungie.api.model.custom

import kotlinx.serialization.Serializable

@Serializable
data class ComponentResult<Data>(
    val data: Data
)