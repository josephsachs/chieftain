package chieftain.game.models.entity.agent

import chieftain.game.models.entity.Polity
import com.chieftain.game.controller.GameChannelController
import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import chieftain.game.action.cache.SharedGameState
import chieftain.game.action.cache.services.MapDataCacheBuilder.Companion.MapCacheItem
import com.chieftain.game.controller.GameOperationController
import com.google.inject.Inject
import com.minare.controller.EntityController
import com.minare.controller.OperationController
import com.minare.core.entity.annotations.*
import com.minare.core.entity.models.Entity
import com.minare.core.operation.models.Operation
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

@EntityType("Clan")
class Clan: Entity(), Agent, Polity {
    private val log = LoggerFactory.getLogger(Clan::class.java)

    @Inject
    private lateinit var entityController: EntityController
    @Inject
    private lateinit var channelController: GameChannelController
    @Inject
    private lateinit var operationController: OperationController
    @Inject
    private lateinit var sharedGameState: SharedGameState

    init {
        type = "Clan"
    }

    @State
    var name: String = ""

    @State
    @Mutable
    var population: Int = 0

    @State
    var culture: CultureGroup = CultureGroup.UNASSIGNED

    @State
    @Mutable
    var location: Pair<Int, Int> = Pair(0, 0)

    @Property
    var behavior: ClanBehavior = ClanBehavior.WANDERING

    @Task
    suspend fun chooseBehavior() {
        if (sharedGameState.isGamePaused()) return

        var deltas = JsonObject()

        deltas
            .put("behavior", ClanBehavior.WANDERING)

        entityController.saveProperties(this._id!!, deltas)
    }

    suspend fun queueWanderAction() {
        broadcastConsole("BEHAVIOR: Clan $name is wandering")

        var x = location.first
        var y = location.second
        var possibles: MutableList<MapCacheItem> = mutableListOf()

        for (n in (x - 1) until (x + 2)) {
            for (m in (x - 1) until (x + 2)) {
                if (n == 0 && m == 0) continue

                log.info("BEHAVIOR: Searching zone $n, $m")

                val item: MapCacheItem? =
                    sharedGameState.mapDataCache.get(n, m)

                if (item == null) {
                    log.info("BEHAVIOR: item was null")
                    return
                }

                log.info("BEHAVIOR: found $item")

                if (item.isPassable) {
                    possibles.add(item)
                }
            }
        }

        if (possibles.isEmpty()) {
            return
        }

        val destination = possibles.random() as MapCacheItem

        broadcastConsole("Clan $name is wandering to $destination")

        val operation = Operation()
            .entity(this._id!!)
            .entityType(Clan::class.java)
            .delta(
                JsonObject()
                    .put("location", Pair(destination.x, destination.y))
            )

        operationController.queue(operation)
    }

    private suspend fun broadcastConsole(message: String) {
        val defaultChannelId = channelController.getDefaultChannel()

        if (defaultChannelId.isNullOrBlank()) {
            return
        }

        channelController.broadcast(
            defaultChannelId,
            JsonObject().put("console", message)
        )
    }

    companion object {
        enum class ClanBehavior(value: String) {
            NONE ("None"),
            WANDERING ("Wandering")
        }
    }
}