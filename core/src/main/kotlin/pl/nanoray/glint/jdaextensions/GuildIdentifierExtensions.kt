package pl.nanoray.glint.jdaextensions

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.requests.restaction.CommandEditAction
import net.dv8tion.jda.api.requests.restaction.MemberAction
import net.dv8tion.jda.api.utils.concurrent.Task

// region Category
fun Guild.getCategory(id: CategoryIdentifier): Category? {
	return getCategoryById(id.value)
}
// endregion

// region Command
fun Guild.retrieveCommand(id: CommandIdentifier): RestAction<Command> {
	return retrieveCommandById(id.value)
}

fun Guild.editCommand(id: CommandIdentifier): CommandEditAction {
	return editCommandById(id.value.toString())
}

fun Guild.deleteCommand(id: CommandIdentifier): RestAction<Unit> {
	return deleteCommandById(id.value.toString()).map { }
}
// endregion

// region Emote
fun Guild.getEmote(id: EmoteIdentifier): Emote? {
	return getEmoteById(id.value)
}

fun Guild.retrieveEmote(id: EmoteIdentifier): RestAction<ListedEmote> {
	return retrieveEmoteById(id.value)
}
// endregion

// region GuildChannel
fun Guild.getGuildChannel(id: GuildChannelIdentifier): GuildChannel? {
	return getGuildChannelById(id.value)
}

fun Guild.getGuildChannel(channelType: ChannelType, id: GuildChannelIdentifier): GuildChannel? {
	return getGuildChannelById(channelType, id.value)
}
// endregion

// region Role
fun Guild.getRole(id: RoleIdentifier): Role? {
	return getRoleById(id.value)
}

fun Guild.getRoleByBot(id: UserIdentifier): Role? {
	return getRoleByBot(id.value)
}
// endregion

// region StoreChannel
fun Guild.getStoreChannel(id: StoreChannelIdentifier): StoreChannel? {
	return getStoreChannelById(id.value)
}
// endregion

// region TextChannel
fun Guild.getTextChannel(id: TextChannelIdentifier): TextChannel? {
	return getTextChannelById(id.value)
}
// endregion

// region User
val Guild.ownerIdentifier: UserIdentifier
	get() = UserIdentifier(ownerIdLong)

fun Guild.addMember(accessToken: String, id: UserIdentifier): MemberAction {
	return addMember(accessToken, id.value)
}

fun Guild.unloadMember(id: UserIdentifier): Boolean {
	return unloadMember(id.value)
}

fun Guild.getMember(id: UserIdentifier): Member? {
	return getMemberById(id.value)
}

fun Guild.retrieveBan(id: UserIdentifier): RestAction<Guild.Ban> {
	return retrieveBanById(id.value)
}

fun Guild.retrieveMember(id: UserIdentifier): RestAction<Member> {
	return retrieveMemberById(id.value)
}

fun Guild.retrieveMember(id: UserIdentifier, update: Boolean): RestAction<Member> {
	return retrieveMemberById(id.value, update)
}

fun Guild.retrieveMembers(ids: Collection<UserIdentifier>): Task<List<Member>> {
	return retrieveMembersByIds(ids.map { it.value })
}

fun Guild.retrieveMembers(includePresence: Boolean, ids: Collection<UserIdentifier>): Task<List<Member>> {
	return retrieveMembersByIds(includePresence, ids.map { it.value })
}

fun Guild.kick(id: UserIdentifier, reason: String? = null): AuditableRestAction<Unit> {
	@Suppress("UNCHECKED_CAST")
	return kick(id.value.toString(), reason) as AuditableRestAction<Unit>
}

fun Guild.ban(id: UserIdentifier, delDays: Int, reason: String? = null): AuditableRestAction<Unit> {
	@Suppress("UNCHECKED_CAST")
	return ban(id.value.toString(), delDays, reason) as AuditableRestAction<Unit>
}

fun Guild.unban(id: UserIdentifier): AuditableRestAction<Unit> {
	@Suppress("UNCHECKED_CAST")
	return unban(id.value.toString()) as AuditableRestAction<Unit>
}

fun Guild.addRoleToMember(id: UserIdentifier, role: Role): AuditableRestAction<Unit> {
	@Suppress("UNCHECKED_CAST")
	return addRoleToMember(id.value, role) as AuditableRestAction<Unit>
}

fun Guild.removeRoleFromMember(id: UserIdentifier, role: Role): AuditableRestAction<Unit> {
	@Suppress("UNCHECKED_CAST")
	return removeRoleFromMember(id.value, role) as AuditableRestAction<Unit>
}
// endregion

// region VoiceChannel
fun Guild.getVoiceChannel(id: VoiceChannelIdentifier): VoiceChannel? {
	return getVoiceChannelById(id.value)
}
// endregion