package pl.nanoray.glint.ranking

import pl.shockah.unikorn.plugin.Plugin
import pl.shockah.unikorn.plugin.PluginInfo
import pl.shockah.unikorn.plugin.PluginManager
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class RankingPlugin(
		manager: PluginManager,
		info: PluginInfo
): Plugin(manager, info), MutableRankingManager {
	private val lock = ReentrantReadWriteLock()
	private val providers = mutableMapOf<RankingType, RankingProvider<*>>()

	override fun getRankingProviderTypes(): Set<RankingType> {
		return lock.read { providers.keys.toSet() }
	}

	@Suppress("UNCHECKED_CAST")
	override fun <Value: Comparable<Value>> getRankingProvider(type: RankingType): RankingProvider<Value>? {
		return lock.read { providers[type]?.let { it as? RankingProvider<Value> } }
	}

	override fun <Value: Comparable<Value>> registerRankingProvider(provider: RankingProvider<Value>) {
		lock.write {
			providers[provider.rankingType] = provider
		}
	}

	override fun <Value: Comparable<Value>> unregisterRankingProvider(provider: RankingProvider<Value>) {
		lock.write {
			if (providers[provider.rankingType] == provider)
				providers.remove(provider.rankingType)
		}
	}
}