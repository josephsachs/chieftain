package chieftain.game.models.entity.agent

import chieftain.game.models.entity.Polity
import com.chieftain.game.models.data.AgentMemory
import com.chieftain.game.models.data.Depot
import com.chieftain.game.models.entity.Culture
import com.chieftain.game.models.entity.Religion
import com.minare.core.entity.annotations.EntityType
import com.minare.core.entity.annotations.Mutable
import com.minare.core.entity.annotations.State
import com.minare.core.entity.models.Entity
import java.util.*

@EntityType("Clan")
class Clan: Entity(), Agent, Polity {
    init {
        type = "Clan"
    }

    @State
    var name: String = ""

    @State
    @Mutable
    var population: Int = 0

    @State
    var culture: Culture.Companion.CultureGroup = Culture.Companion.CultureGroup.UNASSIGNED

    @State
    @Mutable
    var behavior: ClanBehavior = ClanBehavior.WANDERING

    //@State
    //@Mutable
    //@Relationship(type=RelationshipType.PEER)
    //var leader: Character = Character()

    //@State
    //@Mutable
    //@Relationship(type=RelationshipType.PEER)
    //var religion: Religion = Religion()

    //@State
    //@Mutable
    //var depot: Depot = Depot()

    //@State
    //@Mutable
    //var skills: Map<String, Int> = mapOf()

    @State
    @Mutable
    var location: Pair<Int, Int> = Pair(0, 0)

    //@State
    //@Mutable
    //var path: Queue<Pair<Int, Int>>? = null

    //@State
    //@Mutable
    //var memory: AgentMemory = AgentMemory()

    companion object {
        enum class ClanBehavior(value: String) {
            NONE ("None"),
            WANDERING ("Wandering")
        }
    }
}