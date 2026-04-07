package com.chieftain.game.scenario

import chieftain.game.models.data.Vector2
import chieftain.game.models.entity.agent.Character
import chieftain.game.models.entity.agent.Character.Companion.CharacterPersonality
import chieftain.game.models.entity.agent.Character.Companion.CharacterStats
import chieftain.game.models.entity.agent.Character.Companion.CharacterTitle
import chieftain.game.models.entity.agent.Character.Companion.CharacterTraits
import chieftain.game.models.entity.agent.Clan
import chieftain.game.models.entity.agent.Clan.Companion.ClanSkills
import com.chieftain.game.controller.GameChannelController
import com.chieftain.game.models.data.Depot
import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.core.entity.factories.EntityFactory
import com.minare.core.entity.models.Entity
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await

@Singleton
class AgentInitializer @Inject constructor(
    private val gameChannelController: GameChannelController,
    private val entityFactory: EntityFactory,
    private val entityController: EntityController,
    private val vertx: Vertx
) {
    private val log = LoggerFactory.getLogger(AgentInitializer::class.java)

    suspend fun initialize() {
        val entities = mutableListOf<Entity>()
        val defaultChannelId = gameChannelController.getDefaultChannel()

        // Characters must be created first so clans can reference them
        val characterMap = mutableMapOf<String, Character>()

        readJsonFile("scenario/characters.json").forEach { json ->
            val character = entityFactory.createEntity(Character::class.java) as Character
            val id = json.getString("id")
            character._id = "${id}-unsaved"

            character.name = json.getString("name")
            character.culture = CultureGroup.fromString(json.getString("culture"))
            character.title = CharacterTitle.valueOf(json.getString("title"))

            val statsJson = json.getJsonObject("stats")
            character.stats = CharacterStats(
                speech = statsJson.getInteger("speech"),
                peacekeeping = statsJson.getInteger("peacekeeping"),
                fighting = statsJson.getInteger("fighting"),
                pathfinding = statsJson.getInteger("pathfinding"),
                trading = statsJson.getInteger("trading"),
                overseeing = statsJson.getInteger("overseeing"),
                scouting = statsJson.getInteger("scouting"),
                intrigue = statsJson.getInteger("intrigue"),
                mysticism = statsJson.getInteger("mysticism"),
                erudition = statsJson.getInteger("erudition")
            )

            val persJson = json.getJsonObject("personality")
            character.personality = CharacterPersonality(
                cooperatorVsDefector = persJson.getDouble("cooperatorVsDefector"),
                lawfulVsChaotic = persJson.getDouble("lawfulVsChaotic"),
                grandioseVsInsecure = persJson.getDouble("grandioseVsInsecure"),
                riskyVsCautious = persJson.getDouble("riskyVsCautious"),
                ethicalVsAmoral = persJson.getDouble("ethicalVsAmoral"),
                sumptuousVsPrudent = persJson.getDouble("sumptuousVsPrudent")
            )

            val traitsArray = json.getJsonArray("traits")
            character.traits = traitsArray
                .map { CharacterTraits.valueOf(it as String) }
                .toMutableSet()

            entityController.create(character)
            characterMap[id] = character
            entities.add(character)

            log.info("Created character: ${character.name} (${id})")
        }

        // Clans reference their chieftain
        readJsonFile("scenario/agents.json").forEach { json ->
            val clan = entityFactory.createEntity(Clan::class.java) as Clan
            val id = json.getString("id")
            clan._id = "${id}-unsaved"

            clan.name = json.getString("name")
            clan.population = json.getInteger("population")
            clan.culture = CultureGroup.fromString(json.getString("culture"))
            clan.location = Vector2(json.getInteger("x"), json.getInteger("y"))

            val chieftainKey = json.getString("chieftain")
            val chieftain = characterMap[chieftainKey]
            if (chieftain != null) {
                clan.chieftainId = chieftain._id
                clan.chieftain = chieftain
            } else {
                log.warn("Chieftain '${chieftainKey}' not found for clan ${clan.name}")
            }

            val skillsJson = json.getJsonObject("skills")
            clan.skills = ClanSkills(
                gathering = skillsJson.getInteger("gathering"),
                hunting = skillsJson.getInteger("hunting"),
                mining = skillsJson.getInteger("mining"),
                quarrying = skillsJson.getInteger("quarrying"),
                woodcutting = skillsJson.getInteger("woodcutting"),
                sculpture = skillsJson.getInteger("sculpture"),
                jewelry = skillsJson.getInteger("jewelry"),
                scribing = skillsJson.getInteger("scribing"),
                minting = skillsJson.getInteger("minting")
            )

            // Starting food supply
            clan.depot = clan.depot.set(
                Depot.Companion.ResourceTypeGroup.FOOD,
                Depot.Companion.ResourceType.CORN,
                50
            )

            entityController.create(clan)
            entities.add(clan)

            log.info("Created clan: ${clan.name} (${id}), chieftain: ${clan.chieftain?.name}")
        }

        gameChannelController.addEntitiesToChannel(entities.toList(), defaultChannelId!!)
    }

    private suspend fun readJsonFile(path: String): List<JsonObject> {
        return try {
            val buffer = vertx.fileSystem().readFile(path).await()
            JsonArray(buffer.toString()).map { it as JsonObject }
        } catch (e: Exception) {
            log.error("Failed to read $path: $e")
            emptyList()
        }
    }
}
