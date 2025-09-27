package com.chieftain.game.models.entity

import com.minare.core.entity.annotations.EntityType
import com.minare.core.entity.annotations.State

@EntityType("culture")
class Culture {
    @State
    var name: String = ""

    companion object {
        enum class CultureGroup {
            UNASSIGNED,
            CANAANITE,
            HURRIAN,
            SHASU,
            AMORITE,
            HABIRU,
            EGYPTIAN
        }
    }
}