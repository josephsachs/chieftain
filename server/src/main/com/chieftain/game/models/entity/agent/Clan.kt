package chieftain.game.models.entity.agent

import chieftain.game.models.entity.Polity
import com.chieftain.game.controller.GameChannelController
import com.chieftain.game.models.entity.Culture.Companion.CultureGroup
import com.chieftain.game.scenario.GameState
import com.google.inject.Inject
import com.minare.controller.EntityController
import com.minare.core.entity.annotations.*
import com.minare.core.entity.models.Entity
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

@EntityType("Clan")
class Clan: Entity(), Agent, Polity {
    private val log = LoggerFactory.getLogger(Clan::class.java)

    @Inject
    private lateinit var entityController: EntityController
    @Inject
    private lateinit var channelController: GameChannelController
    @Inject
    private lateinit var gameState: GameState

    init {
        type = "Clan"
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
    var location: Pair<Int, Int> = Pair(0, 0)

    @Property
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

    //@State
    //@Mutable
    //var path: Queue<Pair<Int, Int>>? = null

    //@State
    //@Mutable
    //var memory: AgentMemory = AgentMemory()

    @Task
    suspend fun chooseBehavior() {
        if (gameState.isGamePaused()) return

        var deltas = JsonObject()

        deltas
            .put("behavior", ClanBehavior.WANDERING)

        entityController.saveProperties(this._id!!, deltas)

        var msg = "TURN_LOOP: Clan ${this.name} setting properties with ${deltas}"
        log.info(msg)
        try {
            val defaultChannelId = channelController.getDefaultChannel()
            if (defaultChannelId.isNullOrBlank()) {
                log.info("BROADCAST: No default channel ID")
                return
            }
            log.info("BROADCAST: Broadcasting to channel $defaultChannelId")
            channelController.broadcastToChannel(
                defaultChannelId,
                JsonObject().put("console", msg)
            )
        } catch (e: Exception) {
            log.error("BROADCAST: Exception occurred in game task: ${e}")
        }
    }

    companion object {
        enum class ClanBehavior(value: String) {
            NONE ("None"),
            WANDERING ("Wandering")
        }
    }
}