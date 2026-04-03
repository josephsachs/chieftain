package chieftain.game.models.entity.agent

import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import chieftain.game.action.cache.SharedGameState
import chieftain.game.action.cache.services.MapDataCacheBuilder.Companion.MapCacheItem
import chieftain.game.controller.GameMapController
import chieftain.game.models.data.AgentLocationMemory
import chieftain.game.models.data.Vector2
import chieftain.game.models.entity.Combatant
import chieftain.game.models.entity.MapZoneResources
import com.chieftain.game.models.data.Depot
import com.chieftain.game.models.entity.MapZone
import com.google.inject.Inject
import com.minare.controller.EntityController
import com.minare.controller.OperationController
import com.minare.core.entity.annotations.*
import com.minare.core.entity.models.Entity
import com.minare.core.operation.models.Operation
import com.minare.core.operation.models.OperationType
import io.vertx.core.json.JsonObject
import jdk.jshell.spi.ExecutionControl.NotImplementedException
import kotlinx.serialization.json.Json
import org.apache.kafka.common.protocol.types.Field.Bool
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.*
import kotlin.random.Random

@EntityType("Clan")
class Clan: Entity(), Agent, Combatant {
    private val log = LoggerFactory.getLogger(Clan::class.java)

    @Inject
    private lateinit var entityController: EntityController
    @Inject
    private lateinit var operationController: OperationController
    @Inject
    private lateinit var sharedGameState: SharedGameState
    @Inject
    private lateinit var gameMapController: GameMapController

    init {
        type = "Clan"
    }

    @State
    var name: String = ""

    @State
    @Mutable
    var population: Int = 0

    @State
    @Mutable
    var health: ClanHealth = ClanHealth()

    @State
    var culture: CultureGroup = CultureGroup.UNASSIGNED

    @State
    @Mutable
    var location: Vector2 = Vector2(0, 0)

    @State
    @Mutable
    override var combatUnit: CombatUnit = CombatUnit(
        population,
        15,
        5,
        1,
        100,
        20
    )

    @State
    @Mutable
    var depot: Depot = Depot()

    /**
     * AI
     */
    // We periodically execute or reconsider this
    @Property
    var behavior: ClanBehavior = ClanBehavior.WANDERING

    // If we're trying to pathfind
    @State
    @Mutable
    var foodSecurity: Double = 0.00

    // If we're trying to pathfind
    @Property
    var targetNavigation: MutableList<Vector2> = mutableListOf()

    // If we're trying to work
    @Property
    var targetResource: Depot.Companion.ResourceType? = null

    // If we're trying to attack something
    @Property
    @Peer
    var targetCombatant: Combatant? = null

    // This person's eccentricities get final call
    @State
    @Mutable
    var chieftain: Character = Character()

    // Remember our big scores
    @Property
    var locationMemory: AgentLocationMemory = AgentLocationMemory()

    fun haveAnySkills(resources: MapZoneResources): Boolean {
        throw Exception("")
    }

    fun tryChooseResource(resources: MapZoneResources): Depot.Companion.ResourceType? {
        throw Exception("")
    }

    @Task
    suspend fun chooseBehavior() {
        if (sharedGameState.isGamePaused()) return

        // Let's not do this for only 1/3 of entities per tick
        if (Random.nextInt(0, 100) < 97) return

        var dataOutput = JsonObject()
            .put("entityType", "Clan")
            .put("id", _id)
            .put("messageType", "chooseBehavior")

        val hungry = health.satiety < (50 * foodSecurity)

        dataOutput.mergeIn(
            JsonObject()
                .put("hungry", hungry)
        )

        //
        // Figure out if we're near combat as we will want to know this often
        //

        if (hungry) {
            val areaResources = gameMapController
                .getResources(Pair(location.x, location.y))

            if (areaResources.hasFood()) {
                if (haveAnySkills(areaResources) || chieftain.personality.riskyVsCautious > 0.50) {
                    val chosenResource = tryChooseResource(areaResources)

                    if (chosenResource != null) {
                        targetResource = chosenResource
                        behavior = ClanBehavior.LABORING
                        dataOutput.mergeIn(
                            JsonObject()
                                .put("decision", "${chieftain.name} ordered the tribe to gather up ${targetResource}")
                        )
                    } else {
                        behavior = ClanBehavior.WANDERING
                        dataOutput.mergeIn(
                            JsonObject()
                                .put("decision", "${name} decided to keep moving")
                        )
                    }
                } else {
                    val wealth = countWealth()

                    // Pretty arbitrary so not this
                    if (wealth > 100) {
                        dataOutput = goTradeAtMarket(dataOutput)
                    } else {
                        dataOutput = goProduceFood(dataOutput)
                    }
                }
            } else {
                dataOutput = goProduceFood(dataOutput)
            }
        } else {
            dataOutput.mergeIn(
                JsonObject()
                    .put("decision", "${chieftain.name} of ${name} is strategizing")
            )

            //       Now the chieftain's personality matters a lot
            //       do we nav back toward someplace we liked on general principle?
            //       are we flush and going to take it easy with a holiday?
            //       park at the market eating our surplus?
            //       is chieftain a narcissist und need to start fight fur die bigballs?
            //       etc.
        }

        entityController.saveProperties(this._id, JsonObject()
            .put("behavior", behavior))

        log.info("${name} chose behavior: ${dataOutput.toString()}")
    }

    private fun countWealth(): Int {
        return listOf(
            depot.get(Depot.Companion.ResourceTypeGroup.METALS, Depot.Companion.ResourceType.TIN),
            depot.get(Depot.Companion.ResourceTypeGroup.METALS, Depot.Companion.ResourceType.GOLD),
            depot.get(Depot.Companion.ResourceTypeGroup.TREASURE, Depot.Companion.ResourceType.STATUES),
            depot.get(Depot.Companion.ResourceTypeGroup.TREASURE, Depot.Companion.ResourceType.JEWELS),
            depot.get(Depot.Companion.ResourceTypeGroup.TREASURE, Depot.Companion.ResourceType.COINS),
        ).sum()
    }

    private fun goTradeAtMarket(dataOutput: JsonObject): JsonObject {
        locationMemory.memories.forEach { (key, value) ->
            if (value.containsKey(AgentLocationMemory.AgentLocationMemoryType.MARKET)) {
                targetNavigation.clear()
                targetNavigation.add(Vector2(key.x, key.y))
                behavior = ClanBehavior.TRAVELING
                dataOutput.mergeIn(
                    JsonObject()
                        .put("decision", "${chieftain.name} decided ${name} clan should travel")
                        .put("result", "${key.x},${key.y}")
                )

                return@forEach
            }
        }

        return dataOutput
    }

    private fun goProduceFood(dataOutput: JsonObject): JsonObject {
        var foundOne: Boolean = false

        locationMemory.memories.forEach { (key, value) ->
            if (value.containsKey(AgentLocationMemory.AgentLocationMemoryType.HAS_FOOD)) {
                foundOne = true
                targetNavigation.clear()
                targetNavigation.add(Vector2(key.x, key.y))
                behavior = ClanBehavior.TRAVELING
                dataOutput.mergeIn(
                    JsonObject()
                        .put("decision", "${chieftain.name} decided ${name} clan should travel")
                        .put("result", "${key.x},${key.y}")
                )

                return@forEach
            }
        }

        if (!foundOne) {
            doExplore(dataOutput)
        }

        return dataOutput
    }

    private fun doExplore(dataOutput: JsonObject): JsonObject {
        behavior = ClanBehavior.WANDERING
        dataOutput.mergeIn(
            JsonObject()
                .put("decision", "${chieftain.name} sees that ground is unfamiliar: ${name} clan should explore")
        )

        return dataOutput
    }

    suspend fun queueWanderAction() {
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

        operation.build()
        operationController.queue(operation)
    }

    companion object {
        data class ClanHealth (
            var satiety: Int = 100,
            var stress: Int = 100,
            var heart: Int = 100
        ): Serializable

        enum class ClanBehavior(value: String) {
            NONE ("None"),
            WANDERING ("Wandering"),
            TRAVELING ("Traveling"),
            STATIONED ("Stationed"),
            LABORING ("Laboring"),
            FIGHTING ("Fighting"),
            RECOVERING ("Recovering"),
            HOLIDAY ("Holiday")
        }
    }
}