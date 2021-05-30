package pl.nanoray.glint.bungie

import com.github.scribejava.core.builder.api.DefaultApi20
import com.github.scribejava.core.oauth2.clientauthentication.ClientAuthentication
import com.github.scribejava.core.oauth2.clientauthentication.RequestBodyAuthenticationScheme

class BungieOAuthApi: DefaultApi20() {
	override fun getAccessTokenEndpoint(): String {
		return "https://www.bungie.net/platform/app/oauth/token/"
	}

	override fun getAuthorizationBaseUrl(): String {
		return "https://www.bungie.net/en/oauth/authorize"
	}

	override fun getClientAuthentication(): ClientAuthentication {
		return RequestBodyAuthenticationScheme.instance()
	}
}