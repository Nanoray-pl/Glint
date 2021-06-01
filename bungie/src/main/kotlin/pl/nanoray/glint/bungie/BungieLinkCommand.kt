package pl.nanoray.glint.bungie

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import pl.nanoray.glint.http.oauth.OAuth2HttpClientManager
import pl.nanoray.glint.jdaextensions.UserIdentifier
import pl.nanoray.glint.jdaextensions.identifier
import pl.nanoray.glint.slashcommand.SlashCommand
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject

class BungieLinkCommand(
	resolver: Resolver
): SlashCommand.Simple<Unit>(Unit::class) {
	override val name = "bungielink"
	override val description = "Link your Discord account with your Bungie account."

	private val httpClientManager: OAuth2HttpClientManager<UserIdentifier, BungieToken> by resolver.inject()

	override fun handleCommand(event: SlashCommandEvent, options: Unit) {
		val client = httpClientManager.getHttpClient(event.user.identifier)
		val url = client.startAuthorization()
		event.reply("Visit this URL to link your account: $url").setEphemeral(true).queue()
	}
}