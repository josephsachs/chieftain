package com.chieftain.game.models.entity

import com.minare.core.entity.annotations.EntityType
import com.minare.core.entity.annotations.State
import com.minare.core.entity.models.Entity
import java.io.Serializable

@EntityType("MapZone")
class MapZone: Entity(), Serializable {
    init {
        type = "MapZone"
    }

    @State
    var location: Pair<Int, Int> = Pair(0, 0)

    @State
    var terrainType: TerrainType = TerrainType.UNASSIGNED

    companion object {
        enum class TerrainType(val value: String) {
            UNASSIGNED("unassigned"),
            OCEAN("ocean"),
            GRASSLAND("grassland"),
            MEADOW("meadow"),
            SCRUB("scrub"),
            DRYLAND("dryland"),
            WOODLAND("woodland"),
            ROCKLAND("rockland"),
            DESERT("desert"),
            MARSH("marsh");

            companion object {
                fun fromString(value: String): TerrainType {
                    return values().find { it.value == value }
                        ?: throw IllegalArgumentException("Unknown terrain type: $value")
                }
            }

            override fun toString(): String = value
        }

        enum class RawResourceType {
            SOIL,
            CATTLE,
            FOWL,
            FISH,
            REEDS,
            BEES,
            CEDAR,
            GRANITE,
            IRON,
            TIN,
            COPPER,
            GOLD,
            GEMS
        }
    }
}