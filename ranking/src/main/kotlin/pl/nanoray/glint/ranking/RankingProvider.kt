package pl.nanoray.glint.ranking

import pl.nanoray.glint.jdaextensions.UserIdentifier

inline class RankingType(val value: String)

interface RankingProvider<Value: Comparable<Value>> {
	val rankingType: RankingType
	val rankingName: String

	fun getRanking(owner: UserIdentifier): Ranking<Value>?
	fun getLeaderboard(maxResults: Int = 10): List<Ranking<Value>>
}