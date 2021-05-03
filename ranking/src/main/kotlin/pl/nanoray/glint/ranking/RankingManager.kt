package pl.nanoray.glint.ranking

interface RankingManager {
	fun getRankingProviderTypes(): Set<RankingType>
	fun <Value: Comparable<Value>> getRankingProvider(type: RankingType): RankingProvider<Value>?
}

interface MutableRankingManager: RankingManager {
	fun <Value: Comparable<Value>> registerRankingProvider(provider: RankingProvider<Value>)
	fun <Value: Comparable<Value>> unregisterRankingProvider(provider: RankingProvider<Value>)
}