package chieftain.game.action

import chieftain.game.models.entity.Game
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.core.storage.interfaces.StateStore
import com.minare.core.utils.EventStateMachine
import com.minare.core.utils.EventStateMachineContext
import com.minare.core.utils.vertx.EventBusUtils
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.CoroutineScope

// This class now acts as the orchestrator, delegating the complex state management to EventStateMachine.
@Singleton
class GameTurnHandler @Inject constructor(
    private val clanTurnHandler: ClanTurnHandler,
    private val entityController: EntityController,
    private val stateStore: StateStore,
    private val scope: CoroutineScope,
    // EventBusUtils is kept only for the ADDRESS_TURN_COMPLETE signal, not phase transitions.
    private val eventBusUtils: EventBusUtils,
    private val vertx: Vertx
) {
    private var log = LoggerFactory.getLogger(GameTurnHandler::class.java)

    // 💡 NEW: The state machine instance responsible for the phase loop
    private val turnStateMachine: EventStateMachine

    // --- State Machine Actions (Replaces handleAct, handleExecute, handleResolve) ---

    private val actAction: suspend (EventStateMachineContext) -> Unit = { context ->
        log.info("TURN_LOOP: ACT Phase Start")
        // 1. Set game state to ACT and Processing=true
        setGameProperties(TurnPhase.ACT, true)

        // 2. Perform the main business logic
        clanTurnHandler.handleTurn(TurnPhase.ACT)

        // 3. Transition: The action is complete, let external tryNext() handle the move to EXECUTE
    }

    private val executeAction: suspend (EventStateMachineContext) -> Unit = { context ->
        log.info("TURN_LOOP: EXECUTE Phase Start")
        setGameProperties(TurnPhase.EXECUTE, true)

        clanTurnHandler.handleTurn(TurnPhase.EXECUTE)

        // Phase complete, let external tryNext() handle the move to RESOLVE
    }

    private val resolveAction: suspend (EventStateMachineContext) -> Unit = { context ->
        log.info("TURN_LOOP: RESOLVE Phase Start")
        setGameProperties(TurnPhase.RESOLVE, true)

        clanTurnHandler.handleTurn(TurnPhase.RESOLVE)

        // Phase complete, let external tryNext() handle the move to TURN_END
    }

    // The final state action: handles cleanup and increments the turn counter
    private val turnEndAction: suspend (EventStateMachineContext) -> Unit = { _ ->
        log.info("TURN_LOOP: Turn End Start (Cleanup)")

        // 1. Set game properties to not processing
        setGameProperties(null, false)

        // 2. Increment turn counter and save
        incrementGameTurn()

        // 3. Send public signal that the full turn cycle is complete
        eventBusUtils.sendWithTracing(ADDRESS_TURN_COMPLETE, JsonObject())

        // 4. Since the state machine is looping=true, the next tryNext() call will cycle back to ACT_PHASE.
    }


    // --- Initialization Block ---
    init {
        // Initialize the state machine right away
        turnStateMachine = EventStateMachine(
            eventKey = "GAME_TURN_LOOP",
            coroutineScope = scope,
            vertx = vertx,
            looping = true // The sequence must loop indefinitely
        )

        // Register the sequential states (order matters due to LinkedHashMap)
        turnStateMachine.registerState("ACT_PHASE", actAction)
        turnStateMachine.registerState("EXECUTE_PHASE", executeAction)
        turnStateMachine.registerState("RESOLVE_PHASE", resolveAction)
        turnStateMachine.registerState("TURN_END", turnEndAction)

        // Start the state machine immediately (triggers the first state: ACT_PHASE)
        turnStateMachine.start()
    }

    /**
     * Called each frame/tick. It attempts to advance the state ONLY if the previous state is finished.
     * This replaces the complex original handleFrame logic and the asynchronous event listener chain.
     * * NOTE: The original logic in handleFrame was to *start* the next phase if turnProcessing was false.
     * With the state machine, we use tryNext() to attempt to advance the whole loop.
     */
    suspend fun handleFrame() {
        // 💡 Use tryNext() to safely attempt advancing the state machine.
        // This ensures the current state action has completed (isProcessing=false).
        // If the state is running, tryNext() is safely ignored.
        turnStateMachine.tryNext()
    }

    // --- Private Helper Logic (Retained from original class) ---

    private suspend fun setGameProperties(turnPhase: TurnPhase?, isProcessing: Boolean?) {
        val game = getGame()

        val properties = JsonObject()

        if (turnPhase !== null) properties.put("turnPhase", turnPhase.name)
        if (isProcessing !== null) properties.put("turnProcessing", isProcessing)

        entityController.saveProperties(game._id!!, properties)
    }

    private suspend fun incrementGameTurn() {
        val game = getGame()

        log.info("TURN_LOOP: game: id = ${game._id} ; currentTurn = ${game.currentTurn} ; turnPhase = ${game.turnPhase} ; isProcessing = ${game.turnProcessing}")

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
            .findByIds(stateStore.findKeysByType("Game"))
            .firstNotNullOf { it.value } as Game
    }

    // --- Original Companion Object (Kept for compatibility) ---
    companion object {
        const val ADDRESS_TURN_COMPLETE = "turn.handler.turn.complete"
        // Removed ADDRESS_ACT_PHASE_COMPLETE, etc., as they are now managed internally

        // Enum definition is kept here
        enum class TurnPhase { ACT, EXECUTE, RESOLVE }
    }
}