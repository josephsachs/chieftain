package com.chieftain.game.scenario

import com.chieftain.game.controller.GameChannelController
import com.chieftain.game.models.entity.MapZone
import com.chieftain.game.models.entity.MapZone.Companion.TerrainType
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.core.entity.factories.EntityFactory
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await

@Singleton
class MapInitializer @Inject constructor(
    private val gameChannelController: GameChannelController,
    private val entityFactory: EntityFactory,
    private val entityController: EntityController,
    private val vertx: Vertx
) {
    private val log = LoggerFactory.getLogger(MapInitializer::class.java)

    suspend fun initialize() {
        val entities = mutableListOf<MapZone>()
        val defaultChannelId = gameChannelController.getDefaultChannel()

        log.info("CHIEFTAIN: Default channel: $defaultChannelId")

        readMapData().forEach { jsonObject ->
            val mapZone = entityFactory.createEntity(MapZone::class.java) as MapZone

            log.info("CHIEFTAIN: MapData included: $jsonObject")
            log.info("CHIEFTAIN: Factory created mapZone $mapZone")

            mapZone.location = Pair(
                jsonObject.getInteger("x"),
                jsonObject.getInteger("y")
            )

            mapZone.terrainType = TerrainType.fromString(jsonObject.getString("terrainType"))

            entityController.create(mapZone) as MapZone

            entities.add(mapZone)
        }

        gameChannelController.addEntitiesToChannel(entities.toList(), defaultChannelId!!)
    }

    private suspend fun readMapData(): List<JsonObject> {
        return try {
            val buffer = vertx.fileSystem().readFile("scenario/mapzones.json").await()

            val mapZonesArray = JsonArray(buffer.toString())
            mapZonesArray.map { it as JsonObject }
        } catch (e: Exception) {
            log.error("Failed to read mapdata.json: $e")
            emptyList()
        }
    }
}