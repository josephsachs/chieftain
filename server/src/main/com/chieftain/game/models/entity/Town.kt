package com.chieftain.game.models.entity

import chieftain.game.models.entity.agent.Character
import com.chieftain.game.models.data.Building
import com.minare.core.entity.annotations.EntityType
import com.chieftain.game.models.data.Building.Companion.BuildingType
import com.minare.core.entity.annotations.Mutable
import com.minare.core.entity.annotations.State
import com.chieftain.game.models.entity.Culture.Companion.CultureGroup

@EntityType("town")
class Town {
    @State
    @Mutable
    var name: String = ""

    @State
    var buildingType: BuildingType = BuildingType.TOWN

    @State
    @Mutable
    var culture: CultureGroup = CultureGroup.UNASSIGNED

    @State
    var location: Pair<Int, Int> = Pair(0, 0)

    @State
    @Mutable
    var character: Character = Character()
}