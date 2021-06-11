package pl.nanoray.glint.bungie

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import pl.nanoray.glint.bungie.api.model.custom.ManifestId
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface ManifestRepository<Data> {
    val version: String

    fun getDefinition(id: ManifestId<Data>): Data?
}

class JsonStringManifestRepository<Data> @PublishedApi internal constructor (
    override val version: String,
    jsonString: String,
    jsonFormat: Json = Json { ignoreUnknownKeys = true },
    private val mapDataType: KType
): ManifestRepository<Data> {
    companion object {
        inline operator fun <reified Data> invoke(
            version: String,
            jsonString: String,
            jsonFormat: Json = Json { ignoreUnknownKeys = true }
        ): JsonStringManifestRepository<Data> {
            return JsonStringManifestRepository(version, jsonString, jsonFormat, typeOf<Map<ManifestId<Data>, Data>>())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val data: Map<ManifestId<Data>, Data> = jsonFormat.decodeFromString(serializer(mapDataType), jsonString) as Map<ManifestId<Data>, Data>

    override fun getDefinition(id: ManifestId<Data>): Data? {
        return data[id]
    }
}