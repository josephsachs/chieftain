package chieftain.game.models.entity.agent

import chieftain.game.models.entity.Polity
import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import chieftain.game.action.cache.SharedGameState
import chieftain.game.action.cache.services.MapDataCacheBuilder.Companion.MapCacheItem
import chieftain.game.controller.ConsoleController
import chieftain.game.models.data.AgentLocationMemory
import chieftain.game.models.data.Vector2
import com.chieftain.game.models.data.Depot
import com.google.inject.Inject
import com.minare.controller.EntityController
import com.minare.controller.OperationController
import com.minare.core.entity.annotations.*
import com.minare.core.entity.models.Entity
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

    @State
    @Mutable
    var depot: Depot = Depot()

    @Property
    var locationMemory: AgentLocationMemory = AgentLocationMemory()

    @Task
    suspend fun chooseBehavior() {
        if (sharedGameState.isGamePaused()) return

        var deltas = JsonObject()
            .put("behavior", ClanBehavior.WANDERING)

        entityController.saveProperties(this._id, deltas)
    }

    suspend fun queueWanderAction() {
        log.info("WANDER: Wander action began for ${this.name} ${this._id}")
        var possibles: MutableList<MapCacheItem> = mutableListOf()

        for (n in (location.x - 1) until (location.x + 2)) {
            for (m in (location.y - 1) until (location.y + 2)) {
                if (n == 0 && m == 0) continue

                val item: MapCacheItem =
                    sharedGameState.mapDataCache.get(n, m) ?: continue

                if (item.isPassable) {
                    possibles.add(item)
                }
            }
        }
        log.info("WANDER: Wander action found possible destinations ${possibles}")


        if (possibles.isEmpty()) {
            return
        }

        val destination = possibles.random() as MapCacheItem

        val operation = Operation()
            .entity(this._id)
            .version(this.version)
            .entityType(Clan::class)
            .action(OperationType.MUTATE)
            .delta(
                JsonObject()
                    .put("location", Vector2(destination.x, destination.y))
            )

        log.info("WANDER: Operation pre-built ${operation}")

        operation.build()

        log.info("WANDER: Clan ${this.name} built operation ${operation}")

        operationController.queue(operation)
    }

    companion object {
        enum class ClanBehavior(value: String) {
            NONE ("None"),
            WANDERING ("Wandering")
        }
    }
}