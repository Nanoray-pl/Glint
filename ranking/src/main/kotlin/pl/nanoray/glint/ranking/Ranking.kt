package pl.nanoray.glint.ranking

import pl.nanoray.glint.jdaextensions.UserIdentifier

data class Ranking<Value: Comparable<Value>>(
		val provider: RankingProvider<Value>,
		val owner: UserIdentifier,
		val score: Value
)