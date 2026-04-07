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
import org.slf4j.LoggerFactory
import java.io.Serializable

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

    @State
    @Mutable
    var chieftain: Character = Character()

    /**
     * AI
     */
    // These are Properties so we use saveProperties to set them

    // We periodically execute or reconsider this
    @Property
    var behavior: ClanBehavior = ClanBehavior.WANDERING

    // If we're trying to pathfind
    @Property
    var targetNavigation: MutableList<Vector2> = mutableListOf()
    // This is where we would want to go if wishes were horses. We still have to
    // figure out how to get there, but when we do we can add to the top of the list
    // Every time we change our destination we clear this

    // If we're trying to work
    @Property
    var targetResource: Depot.Companion.ResourceType? = null
    // This is what we've chosen to produce with labor

    // If we're trying to attack something
    @Property
    @Peer
    var targetCombatant: String? = null // Entity ID
    // We have an interface, Combatant, to ensure that when something targets something else, that thing
    // has the stat block required for combat. However, we need an entity reference for the actual behavior.
    // For now we implicitly trust this reference.
    // This points at a friction point the framework might be better positioned to address: how to represent
    // interface types in storage and return them to code.
    // Let's explore the pain point by doing it the quick and dirty way.

    // Remember our big scores
    @Property
    var locationMemory: AgentLocationMemory = AgentLocationMemory()

    @Property
    var lastThought: Long = 0L // timestamp

    private fun haveAnySkills(resources: MapZoneResources): Boolean {
        // If we have any skills that apply to resources in this map zone, return true
        throw Exception("")
    }

    private fun tryChooseResource(resources: MapZoneResources): Depot.Companion.ResourceType? {
        // Basic rules:
        // If we have low health.satiety we choose food, always the best (skill * foodValue)
        // If we have a good match of goods to production skills, and there's a marketplace nearby, we
        // prob want to produce these as they will cash out to more food.

        // Later, the chieftain's personality will decide:
        // If high-value goods are present a greedy leader will want to produce them even without skill.
        // If there's nothing too valuable here, a restless leader will want to move on.
        // A deliberate leader will want to end turn and rethink it in the next.
        // An industrious leader will want to produce something even marginal so that the turn isn't wasted.
        // etc.

        // If we got nothing, then we're null and the brain tree knows we aren't going to labor here.
        throw Exception("")
    }

    private fun getMapZoneResources(): MapZoneResources {
        return gameMapController.getResources(Pair(location.x, location.y))
    }

    suspend fun chooseBehavior() {
        if (sharedGameState.isGamePaused()) return

        var dataOutput = JsonObject()
            .put("entityType", "Clan")
            .put("id", _id)
            .put("messageType", "chooseBehavior")

        // Where are we? Are we in a market town? Near combat?

        // If we're in a town, then we have two questions:
        // - Are we not yet done trading?
        // - Are we hanginaround in particular? (Some chieftains want to)
        // Shortcircuit if either applies: Behavior.NONE

        // If we're near combat, then we are deciding whether to engage or avoid.
        // No combat systems yet so we don't have targetCombatant or anything like that
        // to worry about.

        if (health.satiety < 75) {
            dataOutput = handleBehaviorFoodSeeking(dataOutput)

        } else {
            dataOutput.mergeIn(
                JsonObject()
                    .put("decision", "${chieftain.name} of ${name} is strategizing")
            )

            //       Now the chieftain's personality matters a lot
            //       Do we nav back toward someplace we liked on general principle? A rooted leader wants to.
            //       Are we flush and going to take it easy with a holiday?
            //       Do we park at the market eating our surplus? A sumptuous leader wants to.
            //       Does the chieftain start shit for no reason the first time he has a free afternoon?
        }

        entityController.saveProperties(this._id, JsonObject()
            .put("behavior", behavior)
            .put("lastThought", System.currentTimeMillis())
        )

        log.info("${name} chose behavior: ${dataOutput.toString()}")
    }

    private fun handleBehaviorFoodSeeking(dataOutput: JsonObject): JsonObject {
        dataOutput.mergeIn(
            JsonObject()
                .put("decision", "${name} are in search of food")
        )

        val areaResources = getMapZoneResources()

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
                    goTradeAtMarket(dataOutput)
                    // This will lead to buying food
                } else {
                    goProduceFood(dataOutput)
                }
            }
        } else {
            goProduceFood(dataOutput)
        }

        return dataOutput
    }

    private fun countFoodQty(): Int {
        return listOf(
            depot.get(Depot.Companion.ResourceTypeGroup.FOOD, Depot.Companion.ResourceType.CORN),
            depot.get(Depot.Companion.ResourceTypeGroup.FOOD, Depot.Companion.ResourceType.FOWL),
            depot.get(Depot.Companion.ResourceTypeGroup.FOOD, Depot.Companion.ResourceType.FRUIT),
            depot.get(Depot.Companion.ResourceTypeGroup.FOOD, Depot.Companion.ResourceType.MEAT)
        ).sum()
    }

    private fun countFoodValue(): Int {
        return listOf(
            depot.get(Depot.Companion.ResourceTypeGroup.FOOD, Depot.Companion.ResourceType.CORN) * Depot.getFoodValue(Depot.Companion.ResourceType.CORN),
            depot.get(Depot.Companion.ResourceTypeGroup.FOOD, Depot.Companion.ResourceType.FOWL) * Depot.getFoodValue(Depot.Companion.ResourceType.FOWL),
            depot.get(Depot.Companion.ResourceTypeGroup.FOOD, Depot.Companion.ResourceType.FRUIT) * Depot.getFoodValue(Depot.Companion.ResourceType.FRUIT),
            depot.get(Depot.Companion.ResourceTypeGroup.FOOD, Depot.Companion.ResourceType.MEAT) * Depot.getFoodValue(Depot.Companion.ResourceType.MEAT),
        ).sum()
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
        // Here we want to find a market and start navigating there, but that depends on knowing about one.
        // Right now, memories aren't saved.
        dataOutput.mergeIn(
            JsonObject()
                .put("decision", "${name} will try to trade at market")
        )

        locationMemory.memories
            .filter { it.value.containsKey(AgentLocationMemory.AgentLocationMemoryType.MARKET) }
            .forEach { (key, value) ->
                targetNavigation.clear()
                targetNavigation.add(Vector2(key.x, key.y))
                behavior = ClanBehavior.TRAVELING
                dataOutput.mergeIn(
                    JsonObject()
                        .put("decision", "${chieftain.name} decided ${name} clan should travel")
                        .put("result", "${key.x},${key.y}")
                )

                return dataOutput
            }


        dataOutput.mergeIn(
            JsonObject()
                .put("result", "${name} don't know about any markets")
        )

        return dataOutput
    }

    private fun goProduceFood(dataOutput: JsonObject): JsonObject {
        // Therefore, we expect our Clan to behave identically after the current changes:
        // because it has no memories, it should wander randomly.
        // Only now it calls this "exploring" and it's the end result of having eliminated all better options.
        dataOutput.mergeIn(
            JsonObject()
                .put("decision", "${name} will try to produce food")
        )

        locationMemory.memories
            .filter { it.value.containsKey(AgentLocationMemory.AgentLocationMemoryType.HAS_FOOD) }
            .forEach { (key, value) ->
                targetNavigation.clear()
                targetNavigation.add(Vector2(key.x, key.y))
                behavior = ClanBehavior.TRAVELING
                dataOutput.mergeIn(
                    JsonObject()
                        .put("decision", "${chieftain.name} decided ${name} clan should travel")
                        .put("result", "${key.x},${key.y}")
                )

                return dataOutput
            }

        dataOutput.mergeIn(
            JsonObject()
                .put("decision", "${name} doesn't know about any places to find food")
        )
        doExplore(dataOutput)

        return dataOutput
    }

    private fun doExplore(dataOutput: JsonObject): JsonObject {
        behavior = ClanBehavior.WANDERING
        dataOutput.mergeIn(
            JsonObject()
                .put("decision", "${name} are exploring")
        )

        return dataOutput
    }

    suspend fun queueWanderAction() {
        val possibles: MutableList<MapCacheItem> = mutableListOf()

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

    fun dynamics() {
        val foodAmt = countFoodValue()
        val hasEnough = foodAmt - population > 0
        val targetAmt = if (hasEnough) { population } else { foodAmt }

        // We cash out food resources in our Depot to try to reach targetAmt
        // Collected deducted resources for a mutate operation
        // if we have bad satiety (< 0), roll to avoid losing pops to starvation
        // If we have no pops, deactivate with a death message

        // We should use operationController for state changes, so queue deltas, don't saveState
    }

    fun deactivate() {
        // This hides us from clients without deleting us yet
    }

    companion object {
        data class ClanHealth (
            var satiety: Int = 100,
            var stamina: Int = 100,
            var heart: Int = 100
        ): Serializable

        enum class ClanBehavior(value: String) {
            NONE ("None"),
            WANDERING ("Wandering"),
            TRAVELING ("Traveling"),
            LABORING ("Laboring"),
            FIGHTING ("Fighting"),
            RECOVERING ("Recovering"),
            HOLIDAY ("Holiday")
        }
    }
}