package chieftain.game.models.entity.mapfeature

import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import com.minare.core.entity.annotations.EntityType
import com.minare.core.entity.annotations.Mutable
import com.minare.core.entity.annotations.Parent
import com.minare.core.entity.annotations.State
import com.minare.core.entity.models.Entity

@EntityType("town")
class Town: Entity() {
    init {
        type = "town"
    }

    @State
    @Mutable
    var culture: CultureGroup = CultureGroup.UNASSIGNED

    //@State
    //@Mutable
    //var character: Character = Character()

    @State
    @Parent
    var mapFeatureRef: String = ""
}