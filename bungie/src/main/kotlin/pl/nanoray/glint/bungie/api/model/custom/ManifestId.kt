package pl.nanoray.glint.bungie.api.model.custom

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ManifestId<Data>(val id: String)

data class ManifestEntry<Data>(
    val id: ManifestId<Data>,
    val data: Data
)