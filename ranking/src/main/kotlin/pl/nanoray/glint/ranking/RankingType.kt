package pl.nanoray.glint.ranking

data class RankingType(
		val identifier: String,
		val name: String,
		val better: Better
) {
	enum class Better {
		Higher, Lower
	}
}