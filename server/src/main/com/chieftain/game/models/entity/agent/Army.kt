package chieftain.game.models.entity.agent

import chieftain.game.models.data.AgentLocationMemory
import chieftain.game.models.data.Vector2
import chieftain.game.models.entity.Combatant
import chieftain.game.models.entity.Polity
import com.chieftain.game.models.data.Depot
import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import com.google.inject.Inject
import com.minare.controller.EntityController
import com.minare.controller.OperationController
import com.minare.core.entity.annotations.*
import com.minare.core.entity.models.Entity
import org.slf4j.LoggerFactory
import java.util.*

@EntityType("Army")
class Army(): Entity(), Combatant {
    private val log = LoggerFactory.getLogger(Clan::class.java)

    @Inject
    private lateinit var entityController: EntityController
    @Inject
    private lateinit var operationController: OperationController

    init {
        type = "Army"
    }

    @State
    var name: String = ""

    @State
    @Mutable
    var population: Int = 0

    @State
    var culture: CultureGroup = CultureGroup.UNASSIGNED

    @State
    @Mutable
    var location: Vector2 = Vector2(0, 0)

    @Property
    var loyalTo: Polity? = null

    /**
     * AI
     */
    // We periodically execute or reconsider this
    @Property
    var behavior: ArmyBehavior = ArmyBehavior.NONE

    // If we're trying to pathfind
    @State
    @Mutable
    var foodSecurity: Double = 0.00

    // If we're trying to pathfind
    @Property
    var targetNavigation: Queue<Vector2>? = null

    // If we're trying to attack something
    @Property
    @Peer
    var targetCombatant: Combatant? = null

    // This person's eccentricities get final call
    @State
    @Mutable
    var commander: Character = Character()

    @State
    @Mutable
    override var combatUnit: CombatUnit = CombatUnit(
        population,
        15,
        5,
        1,
        100,
        20
    )

    @State
    @Mutable
    var depot: Depot = Depot()

    @Property
    var locationMemory: AgentLocationMemory = AgentLocationMemory()
}

enum class ArmyBehavior {
    NONE,
    PURSUING,
    WITHDRAWING,
    HOLDING,
    BROKEN
}