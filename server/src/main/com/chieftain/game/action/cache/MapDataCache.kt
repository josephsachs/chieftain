package chieftain.game.action.cache

import com.google.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class MapDataCache {
    internal data class Hex(
        val x: Int,
        val y: Int,
        val movementCost: Int,
        val passable: Boolean
    )

    private val cache = ConcurrentHashMap<Pair<Int, Int>, Hex>()
}