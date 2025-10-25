package com.chieftain.game.scenario

import chieftain.game.models.entity.agent.Clan
import com.chieftain.game.controller.GameChannelController
import com.chieftain.game.models.data.AgentMemory
import com.chieftain.game.models.data.Depot
import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import com.chieftain.game.models.entity.MapZone
import com.chieftain.game.models.entity.MapZone.Companion.TerrainType
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.core.entity.factories.EntityFactory
import com.minare.core.entity.models.Entity
import com.minare.core.entity.models.serializable.Vector2
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
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

        readClanData().forEach { jsonObject ->
            val clan = entityFactory.createEntity(Clan::class.java) as Clan
            clan.name = jsonObject.getString("name")

            clan.location = Vector2(
                jsonObject.getInteger("x"),
                jsonObject.getInteger("y")
            )
            clan.culture = CultureGroup.fromString(jsonObject.getString("culture"))
            clan.population = jsonObject.getInteger("population")

            // Use assignment when initializing with EntityController
            clan.depot = clan.depot.set(
                Depot.Companion.ResourceTypeGroup.FOOD,
                Depot.Companion.ResourceType.CORN,
                50
            )

            clan.memory = clan.memory.set(
                AgentMemory.Companion.AgentMemoryTypeGroup.LOCATION,
                AgentMemory.Companion.AgencyMemoryType.ZONE_FAMILIAR,
                10
            )

            entityController.create(clan) as Clan
            entities.add(clan)
        }

        gameChannelController.addEntitiesToChannel(entities.toList(), defaultChannelId!!)
    }

    suspend fun readClanData(): List<JsonObject> {
        return try {
            val buffer = vertx.fileSystem().readFile("scenario/agents.json").await()

            JsonArray(buffer.toString()).map { it as JsonObject }
        } catch (e: Exception) {
            log.error("Failed to read agents.json: $e")
            emptyList()
        }
    }
}