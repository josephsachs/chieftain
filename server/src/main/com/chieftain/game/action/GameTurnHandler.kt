package chieftain.game.action

import chieftain.game.models.entity.Game
import chieftain.game.models.entity.agent.Clan
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.core.storage.interfaces.StateStore
import com.minare.core.utils.vertx.EventBusUtils
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Singleton
class GameTurnHandler @Inject constructor(
    private val clanTurnHandler: ClanTurnHandler,
    private val entityController: EntityController,
    private val stateStore: StateStore,
    private val scope: CoroutineScope,
    private val eventBusUtils: EventBusUtils,
    private val vertx: Vertx
) {
    private var log = LoggerFactory.getLogger(GameTurnHandler::class.java)

    /**
     * Called each frame, pipe should be clear
     */
    suspend fun handleFrame(game: Game) {
        if (!game.turnProcessing) {
            setGameTurnProcessing(true)

            when (game.turnPhase) {
                TurnPhase.ACT -> {
                    handleAct()
                }
                TurnPhase.EXECUTE -> {
                    handleExecute()
                }
                TurnPhase.RESOLVE -> {
                    handleResolve()
                }
            }

            log.info("TURN_LOOP: Handling ${game.turnPhase}")
        }
    }

    /**
     * ACT phase
     */
    suspend fun handleAct() {
        try {
            clanTurnHandler.handleTurn(TurnPhase.ACT)
        } finally {
            eventBusUtils.sendWithTracing(ADDRESS_ACT_PHASE_COMPLETE, JsonObject())
        }
    }

    /**
     * EXECUTE phase
     */
    suspend fun handleExecute() {
        try {
            clanTurnHandler.handleTurn(TurnPhase.EXECUTE)
        } finally {
            eventBusUtils.sendWithTracing(ADDRESS_EXECUTE_PHASE_COMPLETE, JsonObject())
        }
    }

    /**
     * RESOLVE phase
     */
    suspend fun handleResolve() {
        try {
            clanTurnHandler.handleTurn(TurnPhase.RESOLVE)
        } finally {
            eventBusUtils.sendWithTracing(ADDRESS_RESOLVE_PHASE_COMPLETE, JsonObject())
        }
    }

    suspend fun registerEventListeners() {
        vertx.eventBus().consumer<JsonObject>(ADDRESS_ACT_PHASE_COMPLETE, { message ->
            scope.launch {
                setGameProperties(TurnPhase.EXECUTE, false)
            }
        })

        vertx.eventBus().consumer<JsonObject>(ADDRESS_EXECUTE_PHASE_COMPLETE, { message ->
            scope.launch {
                setGameProperties(TurnPhase.RESOLVE, false)
            }
        })

        vertx.eventBus().consumer<JsonObject>(ADDRESS_RESOLVE_PHASE_COMPLETE, { message ->
            scope.launch {
                setGameProperties(TurnPhase.ACT, false)

                eventBusUtils.sendWithTracing(ADDRESS_TURN_COMPLETE, JsonObject())
            }
        })

        vertx.eventBus().consumer<JsonObject>(ADDRESS_TURN_COMPLETE, { message ->
            scope.launch {
                incrementGameTurn()
            }
        })
    }

    private suspend fun setGameProperties(turnPhase: TurnPhase?, isProcessing: Boolean?) {
        val game = getGame()

        val properties = JsonObject()

        if (turnPhase !== null) properties.put("turnPhase", turnPhase)
        if (isProcessing !== null) properties.put("turnProcessing", isProcessing)

        entityController.saveProperties(game._id!!, properties)
    }

    private suspend fun setGameTurnProcessing(isProcessing: Boolean) {
        setGameProperties(null, isProcessing)
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

    companion object {
        const val ADDRESS_TURN_COMPLETE = "turn.handler.turn.complete"
        const val ADDRESS_ACT_PHASE_COMPLETE = "turn.handler.act.phase.complete"
        const val ADDRESS_EXECUTE_PHASE_COMPLETE = "turn.handler.execute.phase.complete"
        const val ADDRESS_RESOLVE_PHASE_COMPLETE = "turn.handler.resolve.phase.complete"

        enum class TurnPhase {
            ACT,
            EXECUTE,
            RESOLVE
        }
    }
}