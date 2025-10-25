package chieftain.game.models.entity.agent

import chieftain.game.models.entity.Polity
import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import chieftain.game.action.cache.SharedGameState
import chieftain.game.action.cache.services.MapDataCacheBuilder.Companion.MapCacheItem
import chieftain.game.controller.ConsoleController
import com.google.inject.Inject
import com.minare.controller.EntityController
import com.minare.controller.OperationController
import com.minare.core.entity.annotations.*
import com.minare.core.entity.models.Entity
import com.minare.core.entity.models.serializable.Vector2
import com.minare.core.operation.models.Operation
import com.minare.core.operation.models.OperationType
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

@EntityType("Clan")
class Clan: Entity(), Agent, Polity {
    private val log = LoggerFactory.getLogger(Clan::class.java)

    @Inject
    private lateinit var entityController: EntityController
    @Inject
    private lateinit var consoleController: ConsoleController
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
    var location: Vector2 = Vector2(0, 0)

    @Property
    var behavior: ClanBehavior = ClanBehavior.WANDERING

    @Task
    suspend fun chooseBehavior() {
        if (sharedGameState.isGamePaused()) return

        var deltas = JsonObject()
            .put("behavior", ClanBehavior.WANDERING)

        entityController.saveProperties(this._id!!, deltas)
    }

    suspend fun queueWanderAction() {
        var x = location.x
        var y = location.y
        var possibles: MutableList<MapCacheItem> = mutableListOf()

        for (n in (x - 1) until (x + 2)) {
            for (m in (y - 1) until (y + 2)) {
                if (n == 0 && m == 0) continue

                val item: MapCacheItem =
                    sharedGameState.mapDataCache.get(n, m) ?: continue

                if (item.isPassable) {
                    possibles.add(item)
                }
            }
        }

        if (possibles.isEmpty()) {
            return
        }

        val destination = possibles.random() as MapCacheItem

        consoleController.broadcast("Clan $name is wandering to ${destination.x}, ${destination.y}")

        val operation = Operation()
            .entity(this._id!!)
            .version(this.version)
            .entityType(Clan::class)
            .action(OperationType.MUTATE)
            .delta(
                JsonObject()
                    .put("location", Vector2(destination.x, destination.y))
            )

        operation.build()

        operationController.queue(operation)
    }

    companion object {
        enum class ClanBehavior(value: String) {
            NONE ("None"),
            WANDERING ("Wandering")
        }
    }
}