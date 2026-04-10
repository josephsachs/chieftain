package chieftain.game.models.entity.agent

import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import chieftain.game.action.cache.SharedGameState
import chieftain.game.action.cache.services.MapDataCacheBuilder.Companion.MapCacheItem
import chieftain.game.controller.GameMapController
import chieftain.game.models.data.AgentLocationMemory
import chieftain.game.models.data.Vector2
import chieftain.game.models.entity.City
import chieftain.game.models.entity.Combatant
import chieftain.game.models.entity.MapZoneResources
import chieftain.game.models.entity.Polity
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
import java.io.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt
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

    @State
    @Mutable
    var chieftainId: String = ""

    @Transient
    var chieftain: Character? = null

    @Transient
    var cityAtLocation: City? = null

    @State
    @Mutable
    var skills: ClanSkills = ClanSkills()

    @State
    @Mutable
    var alignedWith: Polity? = null

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

    @Property
    var leaderDecision: String = ""

    private val chieftainName: String
        get() = chieftain?.name ?: "the clan"

    private val chieftainPersonality: Character.Companion.CharacterPersonality
        get() = chieftain?.personality ?: Character.Companion.CharacterPersonality()

    private fun getAvailableRecipes(resources: MapZoneResources): List<ProductionRecipe> {
        return PRODUCTION_RECIPES.filter { recipe ->
            resources.get(recipe.rawResource) > 0 && skills.getSkill(recipe.skill) > 0
        }
    }

    private fun haveAnySkills(resources: MapZoneResources): Boolean {
        return getAvailableRecipes(resources).isNotEmpty()
    }

    private fun tryChooseResource(resources: MapZoneResources): Depot.Companion.ResourceType? {
        val available = getAvailableRecipes(resources)
        if (available.isEmpty()) return null

        // If we have low satiety, prioritize food — pick the best (skill * foodValue)
        if (health.satiety < 75) {
            val foodRecipes = available.filter { it.outputGroup == Depot.Companion.ResourceTypeGroup.FOOD }
            if (foodRecipes.isNotEmpty()) {
                return foodRecipes.maxByOrNull { recipe ->
                    skills.getSkill(recipe.skill) * Depot.getFoodValue(recipe.output)
                }?.output
            }
        }

        // Otherwise pick what we're best at producing from what's here
        return available.maxByOrNull { recipe ->
            skills.getSkill(recipe.skill) * resources.get(recipe.rawResource)
        }?.output
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

        // Low heart: compulsive wandering until morale recovers
        //if (health.heart < 25 && behavior != ClanBehavior.NONE) {
        //    behavior = ClanBehavior.NONE
        //    leaderDecision = "$name are demoralized and idle"
        //    entityController.saveProperties(this._id, JsonObject()
        //        .put("behavior", behavior)
        //        .put("lastThought", System.currentTimeMillis())
        //        .put("leaderDecision", leaderDecision)
        //    )
        //    log.info("$name forced to reconsider (heart=${health.heart})")
        //    return
       // }

        // Low stamina: force reconsideration next turn
        if (health.stamina < 20) {
            behavior = ClanBehavior.NONE
            leaderDecision = "$name are exhausted and must rest"
            entityController.saveProperties(this._id, JsonObject()
                .put("behavior", behavior)
                .put("lastThought", System.currentTimeMillis())
                .put("leaderDecision", leaderDecision)
            )
            log.info("$name forced to reconsider (stamina=${health.stamina})")
            return
        }

        // Committed to traveling — don't reconsider until we arrive
        if (behavior == ClanBehavior.TRAVELING && targetNavigation.isNotEmpty()) {
            log.info("$name continuing journey (${targetNavigation.size} waypoints remaining)")
            return
        }

        // Are we at a city with a market?
        val city = cityAtLocation
        if (city != null) {
            // Record this market in memory
            locationMemory = locationMemory.setMemory(
                location,
                AgentLocationMemory.AgentLocationMemoryType.MARKET,
                mapOf("city" to 1)
            )

            // If we have goods/metals/treasure to sell, or we need food and have wealth, trade
            val tradableWealth = countTradableValue(city)

            // TEMPORARY DEBUG
            log.info("DEBUG_WEALTH: $tradableWealth")

            val needFood = health.satiety < 90

            if (tradableWealth > 0 || (needFood && countTradeStock() > 0)) {
                behavior = ClanBehavior.TRADING
                leaderDecision = "$chieftainName brings $name to market at ${city.name}"

                entityController.saveProperties(this._id, JsonObject()
                    .put("behavior", behavior)
                    .put("targetResource", targetResource?.name)
                    .put("targetNavigation", targetNavigation)
                    .put("lastThought", System.currentTimeMillis())
                    .put("locationMemory", locationMemory.toJson())
                    .put("leaderDecision", leaderDecision)
                )

                log.info("$name chose behavior: $dataOutput")
                return
            }
        }

        if (health.satiety < 75) {
            dataOutput = handleBehaviorFoodSeeking(dataOutput)
        } else {
            dataOutput = handleBehaviorSurplus(dataOutput)
        }

        leaderDecision = dataOutput.getString("result", dataOutput.getString("decision", ""))

        entityController.saveProperties(this._id, JsonObject()
            .put("behavior", behavior)
            .put("targetResource", targetResource?.name)
            .put("targetNavigation", targetNavigation)
            .put("lastThought", System.currentTimeMillis())
            .put("leaderDecision", leaderDecision)
        )

        log.info("$name chose behavior: $dataOutput")
    }

    /**
     * Current food strategy is failing and morale is slipping.
     * Branch on personality/traits to decide how to change tack.
     * Returns non-null if a crisis response was chosen.
     */
    private fun handleFoodCrisis(dataOutput: JsonObject, areaResources: MapZoneResources): JsonObject? {
        val personality = chieftainPersonality

        // Prudent or greedy chieftain: pivot to producing goods for trade
        if (personality.sumptuousVsPrudent < 0.50 ||
            chieftain?.traits?.contains(Character.Companion.CharacterTraits.GREEDY) == true) {

            val goodsRecipe = PRODUCTION_RECIPES
                .filter { it.outputGroup != Depot.Companion.ResourceTypeGroup.FOOD }
                .filter { areaResources.get(it.rawResource) > 0 && skills.getSkill(it.skill) > 0 }
                .maxByOrNull { skills.getSkill(it.skill) * areaResources.get(it.rawResource) }

            if (goodsRecipe != null) {
                targetResource = goodsRecipe.output
                behavior = ClanBehavior.LABORING
                dataOutput.put("decision",
                    "$chieftainName changed tack — $name will produce ${goodsRecipe.output} to trade for food")
                return dataOutput
            }
        }

        return null
    }

    private fun handleBehaviorFoodSeeking(dataOutput: JsonObject): JsonObject {
        dataOutput.put("decision", "${name} are in search of food")

        val areaResources = getMapZoneResources()

        // 1. Have trade stock and know a market — go sell for food
        if (countTradeStock() > 0 &&
            tryNavigateToMemory(AgentLocationMemory.AgentLocationMemoryType.MARKET, dataOutput)) {
            dataOutput.put("decision", "$chieftainName leads $name to market with goods to trade for food")
            return dataOutput
        }

        // 2. Crisis: starving and morale slipping — current strategy isn't working
        if (health.satiety <= 0 && health.heart < 100) {
            val crisis = handleFoodCrisis(dataOutput, areaResources)
            if (crisis != null) return crisis
        }

        // 2. Can produce something here — tryChooseResource picks food-first when hungry
        if (haveAnySkills(areaResources)) {
            val chosenResource = tryChooseResource(areaResources)
            if (chosenResource != null) {
                targetResource = chosenResource
                behavior = ClanBehavior.LABORING
                dataOutput.put("decision", "$chieftainName ordered the clan to work on ${targetResource.toString().lowercase()}")
                return dataOutput
            }
        }

        // 3. Risky chieftain tries to gather food here despite no skill
        if (chieftainPersonality.riskyVsCautious > 0.50 && areaResources.hasFood()) {
            val bestFood = PRODUCTION_RECIPES
                .filter { it.outputGroup == Depot.Companion.ResourceTypeGroup.FOOD && areaResources.get(it.rawResource) > 0 }
                .maxByOrNull { Depot.getFoodValue(it.output) * areaResources.get(it.rawResource) }

            if (bestFood != null) {
                targetResource = bestFood.output
                behavior = ClanBehavior.LABORING
                dataOutput.put("decision", "$chieftainName told $name to try gathering $targetResource despite inexperience")
                return dataOutput
            }
        }

        // 4. Remember a food source — travel there
        if (tryNavigateToMemory(AgentLocationMemory.AgentLocationMemoryType.HAS_FOOD, dataOutput)) {
            return dataOutput
        }

        // 5. Nothing to work with — wander
        behavior = ClanBehavior.WANDERING
        dataOutput.put("result", "${name} are exploring")
        return dataOutput
    }

    private fun handleBehaviorSurplus(dataOutput: JsonObject): JsonObject {
        dataOutput.mergeIn(
            JsonObject()
                .put("decision", "${chieftainName} of ${name} is strategizing")
        )

        // Now the chieftain's personality matters a lot
        val personality = chieftainPersonality
        val areaResources = getMapZoneResources()

        if (personality.sumptuousVsPrudent > 0.65 && countFoodQty() > population * 2) {
            // Sumptuous leader with surplus — take a holiday
            behavior = ClanBehavior.HOLIDAY
            dataOutput.put("result", "$chieftainName declared a feast day")
        } else if (haveAnySkills(areaResources) && areaResources.resources.any { it.value > 0 }) {
            // There's something to work here — an industrious choice
            val chosenResource = tryChooseResource(areaResources)
            if (chosenResource != null) {
                targetResource = chosenResource
                behavior = ClanBehavior.LABORING
                dataOutput.put("result", "$chieftainName put the clan to work on $targetResource")
            } else {
                behavior = ClanBehavior.WANDERING
                dataOutput.put("result", "${name} wander on")
            }
        } else if (chieftain?.traits!!.contains(Character.Companion.CharacterTraits.RESTLESS)) {
            // Restless leader — keep moving, explore
            behavior = ClanBehavior.WANDERING
            dataOutput.put("result", "$chieftainName wants to see what's over the next hill")
        } else {
            // Default: nothing pressing, wander
            behavior = ClanBehavior.WANDERING
            dataOutput.put("result", "${name} drift onward")
        }
        return dataOutput
    }

    private val FOOD_TYPES = listOf(
        Depot.Companion.ResourceType.CORN,
        Depot.Companion.ResourceType.FOWL,
        Depot.Companion.ResourceType.FRUIT,
        Depot.Companion.ResourceType.MEAT,
        Depot.Companion.ResourceType.HONEY
    )

    private fun countFoodQty(): Int {
        return FOOD_TYPES.sumOf { depot.get(Depot.Companion.ResourceTypeGroup.FOOD, it) }
    }

    private fun countFoodValue(): Int {
        return FOOD_TYPES.sumOf { depot.get(Depot.Companion.ResourceTypeGroup.FOOD, it) * Depot.getFoodValue(it) }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun countTradeStock(): Int {
        return Depot.Companion.ResourceType.entries
            .mapNotNull { type -> getResourceGroup(type)?.let { group -> depot.get(group, type) } }
            .sum()
    }

    private fun tryNavigateToMemory(
        memoryType: AgentLocationMemory.AgentLocationMemoryType,
        dataOutput: JsonObject
    ): Boolean {
        val candidates = locationMemory.memories
            .filter { it.value.containsKey(memoryType) }
            .keys
            .sortedBy { abs(it.x - location.x) + abs(it.y - location.y) }

        for (dest in candidates) {
            val path = gameMapController.findPath(
                Pair(location.x, location.y),
                Pair(dest.x, dest.y)
            )
            if (path.isEmpty()) continue

            targetNavigation.clear()
            path.forEach { targetNavigation.add(Vector2(it.first, it.second)) }
            behavior = ClanBehavior.TRAVELING
            dataOutput.put("decision", "${chieftainName} decided ${name} clan should travel")
            dataOutput.put("result", "${dest.x},${dest.y}")
            return true
        }

        return false
    }

    suspend fun queueLaborAction() {
        val resource = targetResource ?: return

        val recipe = PRODUCTION_RECIPES.find { it.output == resource } ?: return
        val skill = skills.getSkill(recipe.skill)
        val rawAvailable = getMapZoneResources().get(recipe.rawResource)

        if (rawAvailable <= 0) {
            // Resource depleted — fall back to wandering next turn
            behavior = ClanBehavior.WANDERING
            targetResource = null
            entityController.saveProperties(this._id, JsonObject()
                .put("behavior", behavior)
                .put("targetResource", null)
            )
            return
        }

        val populationMod = population.toDouble() *
                // Population modifier considers chieftain's skill
                // with the bulk accounted for by how many of the clan's
                // laboring population are able to work.
                ((0.0002 * chieftain?.stats!!.overseeing) + (0.0042 * health.stamina))

        // Production: yield equals skill level, capped by available raw resource times pop mod
        val yield = minOf(maxOf(skill, 1), rawAvailable) * populationMod.roundToInt()
        val currentAmt = depot.get(recipe.outputGroup, recipe.output)
        val updatedDepot = depot.set(recipe.outputGroup, recipe.output, currentAmt + yield)

        log.info("$name produced $yield ${recipe.output} (skill=$skill, pop=$population)")

        val operation = Operation()
            .entity(this._id)
            .version(this.version)
            .entityType(Clan::class)
            .action(OperationType.MUTATE)
            .delta(JsonObject().put("depot", updatedDepot.toJson()))

        operation.build()
        operationController.queue(operation)
    }

    /**
     * How much wealth could we get by selling our tradable goods at this city?
     */
    private fun countTradableValue(city: City): Int {
        val rates = city.exchangeRates
        var total = 0

        // Goods
        for ((resourceName, rate) in rates.buyRates) {
            val resourceType = try { Depot.Companion.ResourceType.valueOf(resourceName) } catch (_: Exception) { continue }
            val group = getResourceGroup(resourceType) ?: continue
            val qty = depot.get(group, resourceType)
            total += qty * rate
        }

        return total
    }

    private fun getResourceGroup(type: Depot.Companion.ResourceType): Depot.Companion.ResourceTypeGroup? {
        return when (type) {
            Depot.Companion.ResourceType.WOOD, Depot.Companion.ResourceType.PAPYRUS, Depot.Companion.ResourceType.STONE -> Depot.Companion.ResourceTypeGroup.GOODS
            Depot.Companion.ResourceType.IRON, Depot.Companion.ResourceType.COPPER, Depot.Companion.ResourceType.TIN, Depot.Companion.ResourceType.GOLD -> Depot.Companion.ResourceTypeGroup.METALS
            Depot.Companion.ResourceType.JEWELS, Depot.Companion.ResourceType.STATUES, Depot.Companion.ResourceType.COINS, Depot.Companion.ResourceType.BOOKS -> Depot.Companion.ResourceTypeGroup.TREASURE
            else -> null
        }
    }

    /**
     * Sell goods for wealth, then buy food with wealth. Stop when out of goods or
     * wealth drops below a modest reserve.
     */
    suspend fun queueTradeAction() {
        val city = cityAtLocation ?: return
        val rates = city.exchangeRates
        var updatedDepot = depot
        var wealth = 0

        // Phase 1: Sell goods/metals/treasure to the city
        for ((resourceName, rate) in rates.buyRates.entries.sortedByDescending { it.value }) {
            val resourceType = try { Depot.Companion.ResourceType.valueOf(resourceName) } catch (_: Exception) { continue }
            val group = getResourceGroup(resourceType) ?: continue
            val qty = updatedDepot.get(group, resourceType)
            if (qty <= 0) continue

            wealth += qty * rate
            updatedDepot = updatedDepot.set(group, resourceType, 0)
            log.info("$name sold $qty $resourceName at ${city.name} for ${qty * rate} wealth")
        }

        // Phase 2: Buy food with accumulated wealth, prioritizing best value
        val foodToBuy = rates.sellRates.entries.sortedBy { it.value } // cheapest first
        for ((foodName, cost) in foodToBuy) {
            if (wealth < cost) continue
            val foodType = try { Depot.Companion.ResourceType.valueOf(foodName) } catch (_: Exception) { continue }

            val unitsToBuy = wealth / cost
            wealth -= unitsToBuy * cost
            val current = updatedDepot.get(Depot.Companion.ResourceTypeGroup.FOOD, foodType)
            updatedDepot = updatedDepot.set(Depot.Companion.ResourceTypeGroup.FOOD, foodType, current + unitsToBuy)
            log.info("$name bought $unitsToBuy $foodName at ${city.name} for ${unitsToBuy * cost} wealth")
        }

        if (updatedDepot != depot) {
            val operation = Operation()
                .entity(this._id)
                .version(this.version)
                .entityType(Clan::class)
                .action(OperationType.MUTATE)
                .delta(JsonObject().put("depot", updatedDepot.toJson()))

            operation.build()
            operationController.queue(operation)
        }
    }

    suspend fun queueTravelAction() {
        if (targetNavigation.isEmpty()) {
            behavior = ClanBehavior.WANDERING
            entityController.saveProperties(this._id, JsonObject()
                .put("behavior", behavior)
            )
            return
        }

        val nextStep = targetNavigation.first()

        // Are we adjacent to the next waypoint?
        val dx = Math.abs(nextStep.x - location.x)
        val dy = Math.abs(nextStep.y - location.y)

        if (dx <= 1 && dy <= 1) {
            // Move there and pop it off the nav list
            targetNavigation.removeAt(0)

            val item = sharedGameState.mapDataCache.get(nextStep.x, nextStep.y)
            if (item == null || !item.isPassable) {
                // Path blocked — give up and wander
                targetNavigation.clear()
                behavior = ClanBehavior.WANDERING
                entityController.saveProperties(this._id, JsonObject()
                    .put("behavior", behavior)
                    .put("targetNavigation", targetNavigation)
                )
                return
            }

            val operation = Operation()
                .entity(this._id)
                .version(this.version)
                .entityType(Clan::class)
                .action(OperationType.MUTATE)
                .delta(JsonObject().put("location", Vector2(nextStep.x, nextStep.y)))

            operation.build()
            operationController.queue(operation)

            entityController.saveProperties(this._id, JsonObject()
                .put("targetNavigation", targetNavigation)
            )
        } else {
            // Not adjacent — we need pathfinding to get closer
            val path = gameMapController.findPath(
                Pair(location.x, location.y),
                Pair(nextStep.x, nextStep.y)
            )

            if (path.isNotEmpty()) {
                val firstStep = path.first()
                val operation = Operation()
                    .entity(this._id)
                    .version(this.version)
                    .entityType(Clan::class)
                    .action(OperationType.MUTATE)
                    .delta(JsonObject().put("location", Vector2(firstStep.first, firstStep.second)))

                operation.build()
                operationController.queue(operation)
            } else {
                // Can't path there — give up
                targetNavigation.clear()
                behavior = ClanBehavior.WANDERING
                entityController.saveProperties(this._id, JsonObject()
                    .put("behavior", behavior)
                    .put("targetNavigation", targetNavigation)
                )
            }
        }
    }

    suspend fun recordLocationMemory() {
        val resources = getMapZoneResources()

        if (resources.hasFood()) {
            val foodScores = listOf(
                MapZoneResources.RawResourceType.SOIL,
                MapZoneResources.RawResourceType.CATTLE,
                MapZoneResources.RawResourceType.FOWL,
                MapZoneResources.RawResourceType.FISH
            ).filter { resources.get(it) > 0 }
             .associate { it.name to resources.get(it) }

            if (foodScores.isNotEmpty()) {
                locationMemory = locationMemory.setMemory(
                    location, AgentLocationMemory.AgentLocationMemoryType.HAS_FOOD, foodScores
                )
            }
        }

        val metalScores = listOf(
            MapZoneResources.RawResourceType.IRON,
            MapZoneResources.RawResourceType.TIN,
            MapZoneResources.RawResourceType.COPPER,
            MapZoneResources.RawResourceType.GOLD
        ).filter { resources.get(it) > 0 }
         .associate { it.name to resources.get(it) }

        if (metalScores.isNotEmpty()) {
            locationMemory = locationMemory.setMemory(
                location, AgentLocationMemory.AgentLocationMemoryType.HAS_METALS, metalScores
            )
        }

        // Record market if there's a city here
        if (cityAtLocation != null) {
            locationMemory = locationMemory.setMemory(
                location, AgentLocationMemory.AgentLocationMemoryType.MARKET, mapOf("city" to 1)
            )
        }

        // Always mark visited
        locationMemory = locationMemory.setMemory(
            location, AgentLocationMemory.AgentLocationMemoryType.VISITED, mapOf("turn" to 0)
        )

        entityController.saveProperties(this._id, JsonObject()
            .put("locationMemory", locationMemory.toJson())
        )
    }

    suspend fun queueWanderAction() {
        val possibles: MutableList<MapCacheItem> = mutableListOf()

        for (n in (location.x - 1) until (location.x + 2)) {
            for (m in (location.y - 1) until (location.y + 2)) {
                if (n == location.x && m == location.y) continue

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
            .delta(JsonObject().put("location", Vector2(destination.x, destination.y)))

        operation.build()
        operationController.queue(operation)
    }

    suspend fun dynamics() {
        val needed = population

        // Consume food, prioritizing cheapest first to preserve high-value food
        var remaining = needed
        var updatedDepot = depot
        for (foodType in FOOD_TYPES.sortedBy { Depot.getFoodValue(it) }) {
            if (remaining <= 0) break
            val available = updatedDepot.get(Depot.Companion.ResourceTypeGroup.FOOD, foodType)
            if (available <= 0) continue

            val valuePerUnit = Depot.getFoodValue(foodType)
            // How many units do we need to consume to cover 'remaining' value?
            val unitsToConsume = minOf(available, (remaining + valuePerUnit - 1) / valuePerUnit)
            remaining -= unitsToConsume * valuePerUnit
            updatedDepot = updatedDepot.set(Depot.Companion.ResourceTypeGroup.FOOD, foodType, available - unitsToConsume)
        }

        // Update satiety based on how well-fed we are
        val fed = if (needed > 0) ((needed - maxOf(remaining, 0)) * 100) / needed else 100
        val satietyDelta = if (fed >= 100) 10 else fed - 100 // recover slowly when full, drop fast when starving

        val newSatiety = (health.satiety + satietyDelta).coerceIn(0, 100)

        // Stamina dynamics: laboring is tiring, holidays restore
        val staminaDelta = when (behavior) {
            ClanBehavior.LABORING -> -8
            ClanBehavior.HOLIDAY -> 15
            ClanBehavior.WANDERING, ClanBehavior.TRAVELING -> -3
            ClanBehavior.FIGHTING -> -10
            else -> 5
        }
        val newStamina = (health.stamina + staminaDelta).coerceIn(0, 100)

        // Heart dynamics: laboring while starving is demoralizing, holidays lift spirits
        var newHeart = health.heart
        if (behavior == ClanBehavior.LABORING && newSatiety <= 0) {
            newHeart = (newHeart - 15).coerceIn(0, 100)
            log.info("${name} losing heart from laboring while starving (heart: $newHeart)")
        } else if (behavior == ClanBehavior.HOLIDAY) {
            newHeart = (newHeart + 20).coerceIn(0, 100)
        } else if (newHeart < 100) {
            // Slow natural recovery
            newHeart = (newHeart + 8).coerceIn(0, 100)
        }

        // Starvation: if satiety hits 0, slim chance of losing someone each turn
        var popLoss = 0
        if (newSatiety <= 0 && population > 0) {
            if (Random.nextDouble(100.0) < 8.0) {
                popLoss = 1
            }
        }

        // Population growth: well-fed clans may grow; holidays boost the odds
        var popGain = 0
        if (newSatiety >= 80 && population > 0) {
            val growthChance = if (behavior == ClanBehavior.HOLIDAY) 8.0 else 2.0
            if (Random.nextDouble(100.0) < growthChance) {
                popGain = 1
                log.info("${name} gained a member (pop: ${population + 1})")
            }
        }

        val newPop = population - popLoss + popGain
        val newHealth = ClanHealth(newSatiety, newStamina, newHeart)

        val delta = JsonObject()
            .put("depot", updatedDepot.toJson())
            .put("health", JsonObject.mapFrom(newHealth))
            .put("population", newPop)
            .put("combatUnit", JsonObject.mapFrom(combatUnit.copy(forces = newPop)))

        val operation = Operation()
            .entity(this._id)
            .version(this.version)
            .entityType(Clan::class)
            .action(OperationType.MUTATE)
            .delta(delta)

        operation.build()
        operationController.queue(operation)

        if (popLoss > 0) {
            log.info("${name} lost $popLoss to starvation (satiety: $newSatiety)")
        }

        if (newPop <= 0) {
            deactivate()
        }
    }

    private fun deactivate() {
        // TODO: needs framework support (EntityController.deactivate or similar)
        log.info("${name} has perished")
    }

    companion object {
        data class ClanHealth (
            var satiety: Int = 100,
            var stamina: Int = 100,
            var heart: Int = 100
        ): Serializable

        data class ClanSkills(
            var gathering: Int = 0,
            var hunting: Int = 0,
            var mining: Int = 0,
            var quarrying: Int = 0,
            var woodcutting: Int = 0,
            var sculpture: Int = 0,
            var jewelry: Int = 0,
            var scribing: Int = 0,
            var minting: Int = 0
        ): Serializable {
            fun getSkill(skillType: Agent.Companion.SkillType): Int {
                return when (skillType) {
                    Agent.Companion.SkillType.GATHERING -> gathering
                    Agent.Companion.SkillType.HUNTING -> hunting
                    Agent.Companion.SkillType.MINING -> mining
                    Agent.Companion.SkillType.QUARRYING -> quarrying
                    Agent.Companion.SkillType.WOODCUTTING -> woodcutting
                    Agent.Companion.SkillType.SCULPTURE -> sculpture
                    Agent.Companion.SkillType.JEWELRY -> jewelry
                    Agent.Companion.SkillType.SCRIBING -> scribing
                    Agent.Companion.SkillType.MINTING -> minting
                }
            }
        }

        /**
         * Maps a raw resource in a MapZone to the skill needed and the depot resource produced.
         */
        data class ProductionRecipe(
            val rawResource: MapZoneResources.RawResourceType,
            val skill: Agent.Companion.SkillType,
            val output: Depot.Companion.ResourceType,
            val outputGroup: Depot.Companion.ResourceTypeGroup
        )

        val PRODUCTION_RECIPES = listOf(
            ProductionRecipe(MapZoneResources.RawResourceType.SOIL, Agent.Companion.SkillType.GATHERING, Depot.Companion.ResourceType.CORN, Depot.Companion.ResourceTypeGroup.FOOD),
            ProductionRecipe(MapZoneResources.RawResourceType.SOIL, Agent.Companion.SkillType.GATHERING, Depot.Companion.ResourceType.FRUIT, Depot.Companion.ResourceTypeGroup.FOOD),
            ProductionRecipe(MapZoneResources.RawResourceType.CATTLE, Agent.Companion.SkillType.HUNTING, Depot.Companion.ResourceType.MEAT, Depot.Companion.ResourceTypeGroup.FOOD),
            ProductionRecipe(MapZoneResources.RawResourceType.FOWL, Agent.Companion.SkillType.HUNTING, Depot.Companion.ResourceType.FOWL, Depot.Companion.ResourceTypeGroup.FOOD),
            ProductionRecipe(MapZoneResources.RawResourceType.FISH, Agent.Companion.SkillType.GATHERING, Depot.Companion.ResourceType.MEAT, Depot.Companion.ResourceTypeGroup.FOOD),
            ProductionRecipe(MapZoneResources.RawResourceType.BEES, Agent.Companion.SkillType.GATHERING, Depot.Companion.ResourceType.HONEY, Depot.Companion.ResourceTypeGroup.FOOD),
            ProductionRecipe(MapZoneResources.RawResourceType.REEDS, Agent.Companion.SkillType.SCRIBING, Depot.Companion.ResourceType.PAPYRUS, Depot.Companion.ResourceTypeGroup.GOODS),
            ProductionRecipe(MapZoneResources.RawResourceType.CEDAR, Agent.Companion.SkillType.WOODCUTTING, Depot.Companion.ResourceType.WOOD, Depot.Companion.ResourceTypeGroup.GOODS),
            ProductionRecipe(MapZoneResources.RawResourceType.GRANITE, Agent.Companion.SkillType.QUARRYING, Depot.Companion.ResourceType.STONE, Depot.Companion.ResourceTypeGroup.GOODS),
            ProductionRecipe(MapZoneResources.RawResourceType.IRON, Agent.Companion.SkillType.MINING, Depot.Companion.ResourceType.IRON, Depot.Companion.ResourceTypeGroup.METALS),
            ProductionRecipe(MapZoneResources.RawResourceType.TIN, Agent.Companion.SkillType.MINING, Depot.Companion.ResourceType.TIN, Depot.Companion.ResourceTypeGroup.METALS),
            ProductionRecipe(MapZoneResources.RawResourceType.COPPER, Agent.Companion.SkillType.MINING, Depot.Companion.ResourceType.COPPER, Depot.Companion.ResourceTypeGroup.METALS),
            ProductionRecipe(MapZoneResources.RawResourceType.GOLD, Agent.Companion.SkillType.MINING, Depot.Companion.ResourceType.GOLD, Depot.Companion.ResourceTypeGroup.METALS),
            ProductionRecipe(MapZoneResources.RawResourceType.GEMS, Agent.Companion.SkillType.JEWELRY, Depot.Companion.ResourceType.JEWELS, Depot.Companion.ResourceTypeGroup.TREASURE)
        )

        enum class ClanBehavior(value: String) {
            NONE ("None"),
            WANDERING ("Wandering"),
            TRAVELING ("Traveling"),
            LABORING ("Laboring"),
            TRADING ("Trading"),
            FIGHTING ("Fighting"),
            RECOVERING ("Recovering"),
            HOLIDAY ("Holiday")
        }
    }
}