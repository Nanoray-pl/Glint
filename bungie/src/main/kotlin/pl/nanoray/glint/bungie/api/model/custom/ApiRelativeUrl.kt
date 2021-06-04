package pl.nanoray.glint.bungie.api.model.custom

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ApiRelativeUrl(val path: String)