package pl.nanoray.glint.bungie

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import pl.nanoray.glint.bungie.api.model.DestinyInventoryItemDefinition
import pl.nanoray.glint.bungie.api.model.DestinyManifest
import pl.nanoray.glint.bungie.api.model.DestinyStatDefinition
import pl.nanoray.glint.bungie.api.model.custom.ApiRelativeUrl
import pl.nanoray.glint.bungie.api.model.custom.ManifestId
import pl.nanoray.glint.bungie.api.service.ManifestService
import pl.nanoray.glint.http.HttpRequest
import pl.nanoray.glint.http.HttpRequestBuilder
import pl.nanoray.glint.http.SingleHttpClient
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.*
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface ManifestManager {
    val inventoryItems: Single<ManifestRepository<DestinyInventoryItemDefinition>>
    val stats: Single<ManifestRepository<DestinyStatDefinition>>
}

class LatestVersionWithDiskStorageManifestManager(
    private val storagePath: Path,
    private val httpRequestBuilder: HttpRequestBuilder,
    private val httpClient: SingleHttpClient,
    private val manifestService: ManifestService,
    private val baseBungieApiUrl: URL,
    private val initialFetch: Start = Start.RightAway
): ManifestManager {
    enum class Start {
        RightAway, Lazy
    }

    private val lock = ReentrantLock()
    private val jsonFormat = Json { ignoreUnknownKeys = true }
    private var manifest: DestinyManifest? = null

    private val manifestPath = storagePath.resolve("manifest.json")

    override val inventoryItems: Single<ManifestRepository<DestinyInventoryItemDefinition>> by manifestRepositoryDelegate { it.inventoryItem }
    override val stats: Single<ManifestRepository<DestinyStatDefinition>> by manifestRepositoryDelegate { it.stat }

    init {
        if (initialFetch == Start.RightAway) {
            updateVersionIfNeeded()
                .doOnSuccess {
                    listOf(
                        inventoryItems, stats
                    ).forEach { it.subscribe() }
                }
                .subscribe()
        }
    }

    private fun updateVersionIfNeeded(): Single<DestinyManifest> {
        return getManifest() // TODO: update periodically
    }

    private fun getLastManifest(): DestinyManifest? {
        if (manifestPath.exists() && manifestPath.isRegularFile())
            return jsonFormat.decodeFromString(serializer(), manifestPath.readText())
        else
            return null
    }

    private fun getManifest(): Single<DestinyManifest> {
        return Single.defer {
            return@defer lock.withLock {
                manifest?.let { return@withLock Single.just(it) }
                getLastManifest()?.let {
                    manifest = it
                    return@withLock Single.just(it)
                }
                return@withLock manifestService.getManifest()
                    .doOnSuccess {
                        lock.withLock {
                            manifestPath.parent.createDirectories()
                            manifestPath.writeText(jsonFormat.encodeToString(serializer(), it))
                            manifest = it
                        }
                    }
            }
        }
    }

    private inline fun <reified Data> manifestRepositoryDelegate(
        name: String? = null,
        noinline relativeUrlSupplier: (DestinyManifest.JsonWorldComponentContentPaths) -> ApiRelativeUrl
    ): ManifestRepositoryDelegate<Data> {
        return ManifestRepositoryDelegate(name, typeOf<Map<ManifestId<Data>, Data>>(), relativeUrlSupplier)
    }

    internal inner class ManifestRepositoryDelegate<Data> @PublishedApi internal constructor(
        private val name: String? = null,
        private val mapDataType: KType,
        private val relativeUrlSupplier: (DestinyManifest.JsonWorldComponentContentPaths) -> ApiRelativeUrl
    ) {
        private val subject = BehaviorSubject.create<ManifestRepository<Data>>()

        private fun getPath(version: String, name: String): Path {
            return storagePath.resolve("$version/$name.json")
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): Single<ManifestRepository<Data>> {
            return updateVersionIfNeeded()
                .flatMap { manifest ->
                    lock.withLock {
                        val filePath = getPath(manifest.version, name ?: property.name)
                        if (subject.hasValue() && subject.value!!.version == manifest.version) {
                            return@withLock Single.just(subject.value!!)
                        } else {
                            return@withLock Single.defer {
                                return@defer lock.withLock innerLock@ {
                                    if (filePath.exists() && filePath.isRegularFile()) {
                                        return@innerLock Single.just(filePath.readBytes())
                                    } else {
                                        val request = httpRequestBuilder.buildRequest(
                                            HttpRequest.Method.GET,
                                            relativeUrlSupplier(manifest.jsonWorldComponentContentPaths["en"]!!).getUrl(baseBungieApiUrl)
                                        )
                                        return@innerLock httpClient.requestSingle(request)
                                            .map { it.data }
                                            .doOnSuccess {
                                                filePath.parent.createDirectories()
                                                filePath.writeBytes(it)
                                            }
                                    }
                                }
                            }
                                .map { it.decodeToString() }
                                .map {
                                    return@map lock.withLock innerLock@ {
                                        val repository = JsonStringManifestRepository<Data>(manifest.version, it, mapDataType = mapDataType, jsonFormat = jsonFormat)
                                        subject.onNext(repository)
                                        return@innerLock repository
                                    }
                                }
                        }
                    }
                }
        }
    }
}