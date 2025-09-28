package com.chieftain.game

import chieftain.game.models.entity.agent.Clan
import chieftain.game.models.entity.mapfeature.Town
import com.minare.core.entity.factories.EntityFactory
import com.minare.core.entity.models.Entity
import com.chieftain.game.models.Node
import com.chieftain.game.models.entity.MapZone
import com.chieftain.game.models.entity.mapfeature.MapFeature
import org.slf4j.LoggerFactory
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * Game EntityFactory implementation.
 * Updated to remove dependency injection since Entity is now a pure data class.
 */
@Singleton
class GameEntityFactory : EntityFactory {
    private val log = LoggerFactory.getLogger(GameEntityFactory::class.java)

    private val classes: HashMap<String, Class<*>> = HashMap()

    init {
        classes["node"] = Node::class.java
        classes["map_zone"] = MapZone::class.java
        classes["map_feature"] = MapFeature::class.java
        classes["town"] = Town::class.java
        classes["clan"] = Clan::class.java
        classes["entity"] = Entity::class.java

        log.info("Registered entity types: ${classes.keys.joinToString()}")
    }

    override fun useClass(type: String): Class<*>? {
        val normalizedType = type.lowercase()
        val result = classes[normalizedType]
        if (result == null) {
            log.warn("Unknown entity type requested: $type")
        }
        return result
    }

    override fun getNew(type: String): Entity {
        val normalizedType = type.lowercase()
        return when (normalizedType) {
            "node" -> Node()
            "map_zone" -> MapZone()
            "map_feature" -> MapFeature()
            "clan" -> Clan()
            "town" -> Town()
            else -> {
                if (normalizedType != "entity") {
                    log.warn("Unknown entity type requested: $type, returning generic Entity")
                }
                Entity()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Entity> createEntity(entityClass: Class<T>): T {
        return when {
            Node::class.java.isAssignableFrom(entityClass) -> Node() as T
            MapZone::class.java.isAssignableFrom(entityClass) -> MapZone() as T
            MapFeature::class.java.isAssignableFrom(entityClass) -> MapFeature() as T
            Town::class.java.isAssignableFrom(entityClass) -> Town() as T
            Clan::class.java.isAssignableFrom(entityClass) -> Clan() as T
            Entity::class.java.isAssignableFrom(entityClass) -> Entity() as T
            else -> {
                log.warn("Unknown entity class requested: ${entityClass.name}, returning generic Entity")
                Entity() as T
            }
        }
    }

    override fun getTypeNames(): List<String> {
        return classes.keys.toList()
    }

    override fun getTypeList(): List<KClass<*>> {
        return listOf(
            Node::class,
            MapZone::class,
            MapFeature::class,
            Town::class,
            Clan::class,
            Entity::class
        )
    }
}