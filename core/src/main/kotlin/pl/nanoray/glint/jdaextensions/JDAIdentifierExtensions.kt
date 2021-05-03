package pl.nanoray.glint.jdaextensions

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.CommandEditAction

// region Category
fun JDA.getCategory(id: CategoryIdentifier): Category? {
	return getCategoryById(id.value)
}
// endregion

// region Command
fun JDA.retrieveCommand(id: CommandIdentifier): RestAction<Command> {
	return retrieveCommandById(id.value)
}

fun JDA.editCommand(id: CommandIdentifier): CommandEditAction {
	return editCommandById(id.value.toString())
}

fun JDA.deleteCommand(id: CommandIdentifier): RestAction<Void> {
	return deleteCommandById(id.value.toString())
}
// endregion

// region Emote
fun JDA.getEmote(id: EmoteIdentifier): Emote? {
	return getEmoteById(id.value)
}
// endregion

// region Guild
fun JDA.getGuild(id: GuildIdentifier): Guild? {
	return getGuildById(id.value)
}

fun JDA.isUnavailable(id: GuildIdentifier): Boolean {
	return isUnavailable(id.value)
}
// endregion

// region GuildChannel
fun JDA.getGuildChannel(id: GuildChannelIdentifier): GuildChannel? {
	return getGuildChannelById(id.value)
}

fun JDA.getGuildChannel(channelType: ChannelType, id: GuildChannelIdentifier): GuildChannel? {
	return getGuildChannelById(channelType, id.value)
}
// endregion

// region PrivateChannel
fun JDA.getPrivateChannel(id: PrivateChannelIdentifier): PrivateChannel? {
	return getPrivateChannelById(id.value)
}
// endregion

// region Role
fun JDA.getRole(id: RoleIdentifier): Role? {
	return getRoleById(id.value)
}
// endregion

// region StoreChannel
fun JDA.getStoreChannel(id: StoreChannelIdentifier): StoreChannel? {
	return getStoreChannelById(id.value)
}
// endregion

// region TextChannel
fun JDA.getTextChannel(id: TextChannelIdentifier): TextChannel? {
	return getTextChannelById(id.value)
}
// endregion

// region User
fun JDA.retrieveUser(id: UserIdentifier): RestAction<User> {
	return retrieveUserById(id.value)
}

fun JDA.retrieveUser(id: UserIdentifier, update: Boolean): RestAction<User> {
	return retrieveUserById(id.value, update)
}

fun JDA.getUser(id: UserIdentifier): User? {
	return getUserById(id.value)
}

fun JDA.unloadUser(id: UserIdentifier): Boolean {
	return unloadUser(id.value)
}

fun JDA.openPrivateChannel(id: UserIdentifier): RestAction<PrivateChannel> {
	return openPrivateChannelById(id.value)
}
// endregion

// region VoiceChannel
fun JDA.getVoiceChannel(id: VoiceChannelIdentifier): VoiceChannel? {
	return getVoiceChannelById(id.value)
}
// endregion

// region Webhook
fun JDA.retrieveWebhook(id: WebhookIdentifier): RestAction<Webhook> {
	return retrieveWebhookById(id.value)
}
// endregion