package chieftain.game.models.entity.agent

import com.minare.core.entity.annotations.EntityType
import com.minare.core.entity.annotations.Mutable
import com.minare.core.entity.annotations.State
import com.minare.core.entity.models.Entity

@EntityType("Treaty")
class Treaty(): Entity() {
    @State
    @Mutable
    var label: String = ""

    @State
    @Mutable
    var sender: Entity = Entity()

    @State
    @Mutable
    var target: Entity = Entity()

    @State
    @Mutable
    var thirdParty: Entity? = null

    @State
    @Mutable
    var message: String = ""

    @State
    @Mutable
    var concludedOn: Long? = null

    @State
    @Mutable
    var provisions: Set<TreatyPropositionType> = mutableSetOf()
}

enum class TreatyPropositionType(value: String) {
    PEACE("Peace"),
    SHARE_LAND("Share Land"),
    TRIBUTE("Tribute"),
    VASSALAGE("Vassalage"),
    MUTUAL_DEFENSE("Mutual Defense"),
    COALITION("Coalition"),
    MARRIAGE("Marriage")
}