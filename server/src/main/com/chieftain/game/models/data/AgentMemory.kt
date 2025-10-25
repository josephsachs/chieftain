package com.chieftain.game.models.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.minare.core.utils.JsonSerializable
import io.vertx.core.json.JsonObject

data class AgentMemory @JsonCreator constructor(
    @JsonProperty("contents") private val _contents: Map<String, Map<String, Int>>
) : JsonSerializable {

    val contents: Map<AgentMemoryTypeGroup, Map<AgencyMemoryType, Int>>
        get() = _contents.mapKeys { (key, _) ->
            AgentMemoryTypeGroup.valueOf(key)
        }.mapValues { (_, resourceMap) ->
            resourceMap.mapKeys { (resourceKey, _) ->
                AgencyMemoryType.valueOf(resourceKey)
            }
        }

    constructor() : this(
        mapOf(
            AgentMemoryTypeGroup.MAPZONE.name to mapOf(
                AgencyMemoryType.ZONE_FAMILIAR.name to 0,
                AgencyMemoryType.ZONE_HAS_FOOD.name to 0,
                AgencyMemoryType.ZONE_HAS_RAWGOODS.name to 0,
                AgencyMemoryType.ZONE_HAS_LOOT.name to 0,
                AgencyMemoryType.ZONE_DANGEROUS.name to 0
            ),
            AgentMemoryTypeGroup.LOCATION.name to mapOf(
                AgencyMemoryType.MARKETPLACE.name to 0,
                AgencyMemoryType.CITY.name to 0
            ),
            AgentMemoryTypeGroup.CLAN.name to mapOf(
                AgencyMemoryType.CLAN_RELATION.name to 0
            )
        )
    )

    override fun toJson(): JsonObject {
        val json = JsonObject()
        val contentsJson = JsonObject()

        _contents.forEach { (group, resources) ->
            val resourcesJson = JsonObject()
            resources.forEach { (resource, amount) ->
                resourcesJson.put(resource, amount)
            }
            contentsJson.put(group, resourcesJson)
        }

        json.put("contents", contentsJson)
        return json
    }

    fun get(group: AgentMemoryTypeGroup, type: AgencyMemoryType): Int {
        return _contents[group.name]?.get(type.name) ?: 0
    }

    fun set(group: AgentMemoryTypeGroup, type: AgencyMemoryType, amount: Int): AgentMemory {
        val newContents = _contents.toMutableMap()
        val groupMap = (newContents[group.name] ?: emptyMap()).toMutableMap()
        groupMap[type.name] = amount
        newContents[group.name] = groupMap
        return AgentMemory(newContents)
    }

    companion object {
        enum class AgentMemoryTypeGroup {
            MAPZONE,
            LOCATION,
            CLAN
        }

        enum class AgencyMemoryType {
            ZONE_FAMILIAR,
            ZONE_HAS_FOOD,
            ZONE_HAS_RAWGOODS,
            ZONE_HAS_LOOT,
            ZONE_DANGEROUS,
            MARKETPLACE,
            CITY,
            CLAN_RELATION
        }
    }
}