package com.chieftain.game.models.entity

import com.minare.core.entity.annotations.EntityType
import com.minare.core.entity.annotations.State

@EntityType("map_hex")
class MapHex {
    @State
    var location: Pair<Int, Int> = Pair(0, 0)

    @State
    var type: TerrainType = TerrainType.UNASSIGNED

    @State
    var elevation: Int = 0

    @State
    var resources: Map<RawResourceType, Int> = mapOf()

    companion object {
        enum class TerrainType {
            UNASSIGNED,
            OCEAN,
            GRASSLAND,
            MEADOW,
            SCRUB,
            DRYLAND,
            WOODLANDS,
            ROCKLAND,
            DESERT,
            MARSH
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