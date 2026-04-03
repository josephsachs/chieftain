package chieftain.game.models.entity.agent

import chieftain.game.models.entity.Polity
import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import chieftain.game.action.cache.SharedGameState
import chieftain.game.action.cache.services.MapDataCacheBuilder.Companion.MapCacheItem
import chieftain.game.controller.ConsoleController
import chieftain.game.models.data.AgentLocationMemory
import chieftain.game.models.data.Vector2
import chieftain.game.models.entity.Combatant
import com.chieftain.game.models.data.Depot
import com.chieftain.game.scenario.GameInitializer
import com.google.inject.Inject
import com.minare.controller.EntityController
import com.minare.controller.OperationController
import com.minare.core.entity.annotations.*
import com.minare.core.entity.models.Entity
import com.minare.core.operation.models.Operation
import com.minare.core.operation.models.OperationType
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.*

@EntityType("Fight")
class Fight: Entity() {
    private val log = LoggerFactory.getLogger(Fight::class.java)

    @Inject
    private lateinit var entityController: EntityController

    @Inject
    private lateinit var operationController: OperationController

    init {
        type = "Fight"
    }

    @State
    var label: String = ""

    @Property
    @Peer
    var participants: MutableSet<String> = mutableSetOf()

    @Property
    var groups: MutableMap<CombatantGroups, FightScores> = mutableMapOf()

    enum class CombatantGroups(value: String) {
        ATTACKERS("Attackers"),
        DEFENDERS("Defenders"),
        INTERLOPERS("Interlopers")
    }

    fun addParticipant(entityId: String) {
        participants.add(entityId)
    }

    fun addParticipant(entity: Entity) {
        participants.add(entity._id)
    }
}

data class FightScores(
    var combatant: Combatant,
    var intention: CombatantIntention,
    var target: String = "",
    var initiative: Int = 0
): Serializable

enum class CombatantIntention {
    ATTACK,
    DEFEND,
    AVOID
}