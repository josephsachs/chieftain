package chieftain.game.action

import chieftain.game.models.entity.Game
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.core.storage.interfaces.StateStore
import com.minare.core.utils.types.esf.EventStateFlow
import com.minare.core.utils.types.esf.StateFlowContext
import com.minare.core.utils.vertx.EventBusUtils
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

// TODO: Rename this class since it's no longer a handler really
@Singleton
class GameTurnHandler @Inject constructor(
    private val clanTurnHandler: ClanTurnHandler,
    private val mapZoneTurnHandler: MapZoneTurnHandler,
    private val fightTurnHandler: FightTurnHandler,
    private val entityController: EntityController,
    private val stateStore: StateStore,
    private val scope: CoroutineScope,
    private val eventBusUtils: EventBusUtils,
    private val vertx: Vertx
) {
    private var log = LoggerFactory.getLogger(GameTurnHandler::class.java)

    private val turnStateMachine: EventStateFlow = EventStateFlow(
        eventKey = "GAME_TURN_LOOP",
        coroutineScope = scope,
        vertx = vertx,
        looping = true // The sequence must loop indefinitely
    )

    private val actAction: suspend (StateFlowContext) -> Unit = { _ ->
        setGameProperties(TurnPhase.ACT, true)

        var data = JsonObject()
            //.put("map", mapZoneTurnHandler.handleTurn(TurnPhase.ACT))
            .put("clans", clanTurnHandler.handleTurn(TurnPhase.ACT))
            .put("fights", fightTurnHandler.handleTurn(TurnPhase.ACT))

        eventBusUtils.publishWithTracing("ADDRESS_TURN_ACT_DATA", data)
    }

    private val executeAction: suspend (StateFlowContext) -> Unit = { _ ->
        log.info("TURN_LOOP: EXECUTE Phase Start")
        setGameProperties(TurnPhase.EXECUTE, true)
        var data = JsonObject()
            //.put("map", mapZoneTurnHandler.handleTurn(TurnPhase.EXECUTE))
            .put("clans", clanTurnHandler.handleTurn(TurnPhase.EXECUTE))
            .put("fights", fightTurnHandler.handleTurn(TurnPhase.EXECUTE))

        eventBusUtils.publishWithTracing("ADDRESS_TURN_EXECUTE_DATA", data)
    }

    private val resolveAction: suspend (StateFlowContext) -> Unit = { _ ->
        log.info("TURN_LOOP: RESOLVE Phase Start")
        setGameProperties(TurnPhase.RESOLVE, true)
        var data = JsonObject()
           // .put("map", mapZoneTurnHandler.handleTurn(TurnPhase.RESOLVE))
            .put("clans", clanTurnHandler.handleTurn(TurnPhase.RESOLVE))
            .put("fights", fightTurnHandler.handleTurn(TurnPhase.RESOLVE))

        eventBusUtils.publishWithTracing("ADDRESS_TURN_RESOLVE_DATA", data)
    }

    private val turnEndAction: suspend (StateFlowContext) -> Unit = { _ ->
        log.info("TURN_LOOP: Turn End Start (Cleanup)")

        setGameProperties(null, false)
        incrementGameTurn()

        eventBusUtils.sendWithTracing(ADDRESS_TURN_COMPLETE, JsonObject())

        // Since the state machine is looping=true, the next tryNext() call
        // will cycle back to ACT_PHASE.
    }

    init {
        turnStateMachine.registerState("ACT_PHASE", actAction)
        turnStateMachine.registerState("EXECUTE_PHASE", executeAction)
        turnStateMachine.registerState("RESOLVE_PHASE", resolveAction)
        turnStateMachine.registerState("TURN_END", turnEndAction)

        // TODO: Have invoking class say when
        turnStateMachine.start()
    }

    private var phaseFrameCount = 0

    /**
     * Called each frame/tick. Enforces a minimum number of idle frames between
     * phase transitions so that queued operations have time to be applied.
     */
    suspend fun handleFrame() {
        phaseFrameCount++
        if (phaseFrameCount >= MIN_FRAMES_PER_PHASE) {
            turnStateMachine.tryNext()
            phaseFrameCount = 0
        }
    }

    private suspend fun setGameProperties(turnPhase: TurnPhase?, isProcessing: Boolean?) {
        val game = getGame()

        val properties = JsonObject()

        if (turnPhase !== null) properties.put("turnPhase", turnPhase.name)
        if (isProcessing !== null) properties.put("turnProcessing", isProcessing)

        entityController.saveProperties(game._id, properties)
    }

    private suspend fun incrementGameTurn() {
        val game = getGame()

        val properties = JsonObject().put("currentTurn", (game.currentTurn + 1))

        try {
            entityController.saveProperties(game._id!!, properties)
        }
        finally {
            val gameTest = getGame()
            log.info("TURN_LOOP: New turn ${gameTest.currentTurn}")
        }
    }

    private suspend fun getGame(): Game {
        return entityController
            .findByIds(stateStore.findAllKeysForType("Game"))
            .firstNotNullOf { it.value } as Game
    }

    companion object {
        const val ADDRESS_TURN_COMPLETE = "turn.handler.turn.complete"
        const val MIN_FRAMES_PER_PHASE = 5

        enum class TurnPhase { ACT, EXECUTE, RESOLVE }
    }
}