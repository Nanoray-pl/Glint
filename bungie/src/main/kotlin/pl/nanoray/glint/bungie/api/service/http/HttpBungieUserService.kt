package pl.nanoray.glint.bungie.api.service.http

import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import pl.nanoray.glint.bungie.api.model.DestinyProfileResponse
import pl.nanoray.glint.bungie.api.model.MembershipType
import pl.nanoray.glint.bungie.api.model.UserMembershipData
import pl.nanoray.glint.bungie.api.model.custom.MembershipId
import pl.nanoray.glint.bungie.api.service.BungieUserService
import pl.nanoray.glint.bungie.api.service.http.model.BungieMembershipPathParameters
import pl.nanoray.glint.http.Endpoint
import pl.nanoray.glint.http.HttpRequestBuilder
import pl.nanoray.glint.http.SingleHttpClient
import pl.nanoray.glint.http.create

/**
 * All methods require authorization.
 */
class HttpBungieUserService(
    client: SingleHttpClient,
    requestBuilder: HttpRequestBuilder,
    jsonFormat: Json,
    private val getMembershipsForCurrentUserEndpoint: Endpoint<Void, Void>,
    private val getProfileEndpoint: Endpoint<BungieMembershipPathParameters, GetProfileQueryParameters>
): BaseHttpBungieService(client, requestBuilder, jsonFormat), BungieUserService {
    @Serializable(with = GetProfileQueryParameters.Serializer::class)
    data class GetProfileQueryParameters(
        val components: Set<DestinyProfileResponse.Component>
    ) {
        object Serializer: KSerializer<GetProfileQueryParameters> {
            private val stringSerializer = String.serializer()
            override val descriptor = stringSerializer.descriptor

            override fun serialize(encoder: Encoder, value: GetProfileQueryParameters) {
                stringSerializer.serialize(encoder, value.components.joinToString(",") { "${it.id}" })
            }

            override fun deserialize(decoder: Decoder): GetProfileQueryParameters {
                val components = stringSerializer.deserialize(decoder)
                    .split(",")
                    .map { it.toInt() }
                    .map { id -> DestinyProfileResponse.Component.values().first { it.id == id } }
                    .toSet()
                return GetProfileQueryParameters(components)
            }
        }
    }

    override fun getMembershipsForCurrentUser(): Single<UserMembershipData> {
        return client.requestSingle(requestBuilder.buildRequest(
            getMembershipsForCurrentUserEndpoint.method,
            getMembershipsForCurrentUserEndpoint.url.create()
        )).deserialize()
    }

    override fun getProfile(
        membershipType: MembershipType,
        membershipId: MembershipId,
        components: Collection<DestinyProfileResponse.Component>
    ): Single<DestinyProfileResponse> {
        return client.requestSingle(requestBuilder.buildRequest(
            getProfileEndpoint.method,
            getProfileEndpoint.url.create(
                BungieMembershipPathParameters(membershipType, membershipId),
                GetProfileQueryParameters(components.toSet())
            )
        )).deserialize()
    }
}