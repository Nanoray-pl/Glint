package pl.nanoray.glint.bungie.api.service

import io.reactivex.rxjava3.core.Single
import pl.nanoray.glint.bungie.api.model.DestinyManifest

interface ManifestService {
    fun getManifest(): Single<DestinyManifest>
}