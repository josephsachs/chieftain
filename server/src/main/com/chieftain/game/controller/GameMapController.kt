package chieftain.game.controller

import chieftain.game.action.cache.SharedGameState
import chieftain.game.models.entity.MapZoneResources
import com.google.inject.Inject
import com.google.inject.Singleton
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.max

@Singleton
class GameMapController @Inject constructor(
    private val sharedGameState: SharedGameState
) {
    companion object {
        private const val MAX_SEARCH_NODES = 2000

        private val DIRECTIONS = listOf(
            -1 to -1, -1 to 0, -1 to 1,
             0 to -1,           0 to 1,
             1 to -1,  1 to 0,  1 to 1
        )
    }

    fun valid(from: Pair<Int, Int>): Boolean {
        val item = sharedGameState.mapDataCache.get(from.first, from.second)
        return item?.isPassable == true
    }

    fun getResources(from: Pair<Int, Int>): MapZoneResources {
        val item = sharedGameState.mapDataCache.get(from.first, from.second)
        return item!!.resources
    }

    /**
     * A* pathfinding over the map cache. Returns the sequence of tiles from
     * start (exclusive) to goal (inclusive), or empty if unreachable.
     * Uses Chebyshev distance as heuristic (8-directional movement).
     */
    fun findPath(from: Pair<Int, Int>, to: Pair<Int, Int>): List<Pair<Int, Int>> {
        if (from == to) return emptyList()
        if (!valid(to)) return emptyList()

        data class Node(val x: Int, val y: Int, val g: Int, val f: Int)

        val open = PriorityQueue<Node>(compareBy { it.f })
        val gScore = mutableMapOf(from to 0)
        val cameFrom = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()

        fun heuristic(a: Pair<Int, Int>) = max(abs(a.first - to.first), abs(a.second - to.second))

        open.add(Node(from.first, from.second, 0, heuristic(from)))

        var explored = 0
        while (open.isNotEmpty() && explored < MAX_SEARCH_NODES) {
            val current = open.poll()
            val pos = current.x to current.y
            explored++

            if (pos == to) {
                val path = mutableListOf<Pair<Int, Int>>()
                var step = to
                while (step != from) {
                    path.add(step)
                    step = cameFrom[step] ?: break
                }
                path.reverse()
                return path
            }

            if (current.g > (gScore[pos] ?: Int.MAX_VALUE)) continue

            for ((dx, dy) in DIRECTIONS) {
                val nx = current.x + dx
                val ny = current.y + dy
                val neighbor = nx to ny

                val tile = sharedGameState.mapDataCache.get(nx, ny)
                if (tile == null || !tile.isPassable) continue

                val tentativeG = current.g + 1
                if (tentativeG < (gScore[neighbor] ?: Int.MAX_VALUE)) {
                    gScore[neighbor] = tentativeG
                    cameFrom[neighbor] = pos
                    open.add(Node(nx, ny, tentativeG, tentativeG + heuristic(neighbor)))
                }
            }
        }

        return emptyList()
    }
}