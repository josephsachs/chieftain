package com.chieftain.game.models.entity

import com.chieftain.game.models.entity.MapZone.Companion.TerrainType
import com.minare.core.entity.annotations.EntityType
import com.minare.core.entity.annotations.State

@EntityType("culture")
class Culture {
    @State
    var name: String = ""

    companion object {
        enum class CultureGroup(val value: String) {
            UNASSIGNED("unassigned"),
            CANAANITE("Canaanite"),
            HURRIAN("Hurrian"),
            SHASU("Shasu"),
            AMORITE("Amorite"),
            HABIRU("Habiru"),
            EGYPTIAN("Egyptian");

            companion object {
                fun fromString(value: String): CultureGroup {
                    return CultureGroup.values().find { it.value == value }
                        ?: throw IllegalArgumentException("Unknown culture group: $value")
                }
            }

            override fun toString(): String = value
        }
    }
}