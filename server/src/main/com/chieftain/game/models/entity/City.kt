package chieftain.game.models.entity

import chieftain.game.models.data.Vector2
import com.chieftain.game.models.data.Depot
import com.chieftain.game.models.entity.Culture
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
    var name: String = ""

    @State
    var location: Vector2 = Vector2(0, 0)

    @State
    @Mutable
    var population: Int = 0

    @State
    @Mutable
    var culture: Culture.Companion.CultureGroup = Culture.Companion.CultureGroup.UNASSIGNED

    @State
    @Mutable
    @Parent
    var alignedWith: Polity? = null

    @State
    @Mutable
    var market: Depot = Depot()
}