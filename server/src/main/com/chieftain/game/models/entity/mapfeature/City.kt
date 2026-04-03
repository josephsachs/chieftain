package chieftain.game.models.entity.mapfeature

import chieftain.game.models.entity.Polity
import com.chieftain.game.models.data.Depot
import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import com.minare.core.entity.annotations.EntityType
import com.minare.core.entity.annotations.Mutable
import com.minare.core.entity.annotations.Parent
import com.minare.core.entity.annotations.State
import com.minare.core.entity.models.Entity

@EntityType("City")
class City: Entity() {
    init {
        type = "City"
    }

    @State
    @Mutable
    var name: String = ""

    @State
    @Mutable
    var population: Int = 0

    @State
    @Mutable
    var culture: CultureGroup = CultureGroup.UNASSIGNED

    @State
    @Mutable
    @Parent
    var alignedWith: Polity = Polity()

    @State
    @Mutable
    var market: Depot = Depot()
}