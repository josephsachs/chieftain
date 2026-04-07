package com.chieftain.game.scenario

import chieftain.game.models.data.Vector2
import chieftain.game.models.entity.MapZoneResources
import chieftain.game.models.entity.City
import com.chieftain.game.controller.GameChannelController
import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import com.chieftain.game.models.entity.MapZone
import com.chieftain.game.models.entity.MapZone.Companion.TerrainType
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
import kotlin.random.Random

@Singleton
class MapInitializer @Inject constructor(
    private val gameChannelController: GameChannelController,
    private val entityFactory: EntityFactory,
    private val entityController: EntityController,
    private val vertx: Vertx
) {
    private val log = LoggerFactory.getLogger(MapInitializer::class.java)

    suspend fun initialize() {
        val entities = mutableListOf<Entity>()
        val defaultChannelId = gameChannelController.getDefaultChannel()

        log.info("Set default channel: $defaultChannelId")

        readJsonFile("scenario/mapzones.json").forEach { jsonObject ->
            val mapZone = entityFactory.createEntity(MapZone::class.java) as MapZone
            mapZone.location = Vector2(
                jsonObject.getInteger("x"),
                jsonObject.getInteger("y")
            )
            mapZone.terrainType = TerrainType.fromString(jsonObject.getString("terrainType"))

            when (mapZone.terrainType) {
                TerrainType.MEADOW -> {
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.SOIL, 5)
                    if (Random.nextBoolean()) {
                        mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.BEES, 1)
                    }
                    if (Random.nextBoolean()) {
                        mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.CATTLE, 1)
                    } else {
                        mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.FOWL, 1)
                    }
                }
                TerrainType.MARSH -> {
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.FOWL, 5)
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.REEDS, 5)
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.FISH, 2)
                }
                TerrainType.DRYLAND -> {
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.CATTLE, 2)
                }
                TerrainType.GRASSLAND -> {
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.SOIL, 5)
                    if (Random.nextBoolean()) {
                        mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.CATTLE, 1)
                    } else {
                        mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.FOWL, 1)
                    }
                    if (Random.nextBoolean()) {
                        mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.BEES, 1)
                    }
                }
                TerrainType.WOODLAND -> {
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.SOIL, 2)
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.FOWL, 1)
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.CEDAR, 3)
                    if (Random.nextBoolean()) {
                        mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.BEES, 1)
                    }
                }
                TerrainType.ROCKLAND -> {
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.CEDAR, 1)
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.GRANITE, 1)
                }
                TerrainType.SCRUB -> {
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.SOIL, 2)
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.FOWL, 1)
                    if (Random.nextBoolean()) {
                        mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.BEES, 1)
                    }
                }
                TerrainType.DESERT -> {
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.SOIL, 1)
                    mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.FOWL, 1)
                    if (Random.nextBoolean()) {
                        mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.BEES, 1)
                    }
                }
                else -> {}
            }

            if (Random.nextBoolean()) {
                mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.COPPER, 1)
            }

            if (Random.nextBoolean()) {
                mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.GRANITE, 1)
            }
            if (Random.nextBoolean() && Random.nextBoolean()) {
                mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.TIN, 1)
            }
            if (Random.nextBoolean()) {
                mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.IRON, 2)
            }
            if (Random.nextBoolean() && Random.nextBoolean()) {
                mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.GOLD, 1)
            }
            if (Random.nextBoolean() && Random.nextBoolean()) {
                mapZone.resources = mapZone.resources.set(MapZoneResources.RawResourceType.GEMS, 1)
            }

            entityController.create(mapZone) as MapZone
            entities.add(mapZone)
        }

        readJsonFile("scenario/cities.json").forEach { jsonObject ->
            val city = entityFactory.createEntity(City::class.java) as City
            val id = jsonObject.getString("id")
            city._id = "${id}-unsaved"

            city.name = jsonObject.getString("name")
            city.population = jsonObject.getInteger("population")
            city.culture = CultureGroup.fromString(jsonObject.getString("culture"))
            city.location = Vector2(
                jsonObject.getInteger("x"),
                jsonObject.getInteger("y")
            )

            entityController.create(city)
            entities.add(city)

            log.info("Created city: ${city.name} (${id})")
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
