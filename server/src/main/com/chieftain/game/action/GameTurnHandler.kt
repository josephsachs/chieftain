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
        // TEMPORARY DEBUG
        log.info("TURN_LOOP: Handling frame, ${game.turnPhase} phase processing is ${game.turnProcessing}")

        if (!game.turnProcessing) {
            // TEMPORARY DEBUG
            log.info("TURN_LOOP: Handling ${game.turnPhase}")

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
        }
    }

    /**
     * ACT phase
     */
    suspend fun handleAct() {
        val clans = entityController.findByIds(
            stateStore.findKeysByType("Clan")
        )

        log.info("clans acting: ${clans.size}")

        try {
            for ((key, clan) in clans) {
                log.info("clan: ${key}")

                clanTurnHandler.handleTurn(clan as Clan)
            }
        } finally {
            eventBusUtils.publishWithTracing(
                ADDRESS_ACT_PHASE_COMPLETE,
                JsonObject()
                    .put("Clans", clans.size)
            )
        }
    }

    /**
     * EXECUTE phase
     */
    suspend fun handleExecute() {
        val clans = entityController.findByIds(
            stateStore.findKeysByType("Clan")
        )

        log.info("clans executing: ${clans.size}")

        try {
            for ((key, clan) in clans) {
                log.info("clan: ${key}")

                clanTurnHandler.handleTurn(clan as Clan)
            }
        } finally {
            eventBusUtils.publishWithTracing(
                ADDRESS_EXECUTE_PHASE_COMPLETE,
                JsonObject()
                    .put("Clans", clans.size)
            )
        }
    }

    /**
     * RESOLVE phase
     */
    suspend fun handleResolve() {
        val clans = entityController.findByIds(
            stateStore.findKeysByType("Clan")
        )

        log.info("clans resolving: ${clans.size}")

        try {
            for ((key, clan) in clans) {
                log.info("clan: ${key}")

                clanTurnHandler.handleTurn(clan as Clan)
            }
        } finally {
            eventBusUtils.publishWithTracing(
                ADDRESS_RESOLVE_PHASE_COMPLETE,
                JsonObject()
                    .put("Clans", clans.size)
            )
            eventBusUtils.publishWithTracing(
                ADDRESS_TURN_COMPLETE,
                JsonObject()
                    .put("Clans", clans.size)
            )
        }
    }

    suspend fun registerEventListeners() {
        // TEMPORARY DEBUG
        log.info("TURN_LOOP: Registering events")

        vertx.eventBus().consumer<JsonObject>(ADDRESS_ACT_PHASE_COMPLETE, { message ->
            // TEMPORARY DEBUG
            log.info("TURN_LOOP: Received act complete event")
            scope.launch {
                setGameProperties(TurnPhase.EXECUTE, false)
            }
        })

        vertx.eventBus().consumer<JsonObject>(ADDRESS_EXECUTE_PHASE_COMPLETE, { message ->
            // TEMPORARY DEBUG
            log.info("TURN_LOOP: Received execute complete event")
            scope.launch {
                setGameProperties(TurnPhase.RESOLVE, false)
            }
        })

        vertx.eventBus().consumer<JsonObject>(ADDRESS_RESOLVE_PHASE_COMPLETE, { message ->
            // TEMPORARY DEBUG
            log.info("TURN_LOOP: Received resolve complete event")
            scope.launch {
                setGameProperties(TurnPhase.ACT, false)
            }
        })

        vertx.eventBus().consumer<JsonObject>(ADDRESS_TURN_COMPLETE, { message ->
            // TEMPORARY DEBUG
            log.info("TURN_LOOP: Received turn complete event")
            scope.launch {
                incrementGameTurn()
            }
        })
    }

    private suspend fun setGameProperties(turnPhase: TurnPhase?, isProcessing: Boolean?) {
        if (turnPhase == null && isProcessing == null) return

        val game = entityController
            .findByIds(stateStore.findKeysByType("Game"))
            .firstNotNullOf { it.value } as Game

        // TEMPORARY DEBUG
        log.info("TURN_LOOP: Got game with id ${game._id}")
        // TEMPORARY DEBUG
        log.info("TURN_LOOP: Setting properties ${turnPhase} processing to ${isProcessing}")

        val properties = JsonObject()

        if (turnPhase !== null) properties.put("turnPhase", turnPhase)
        if (isProcessing !== null) properties.put("turnProcessing", isProcessing)

        entityController.saveProperties(game._id!!, properties)

        // TEMPORARY DEBUG
        val foundEntity = entityController.findByIds(listOf(game._id!!)).firstNotNullOf { it.value } as Game
        log.info("TURN_LOOP: After save found entity ${foundEntity._id} with ${foundEntity.turnPhase} processing is ${foundEntity.turnProcessing}")
    }

    private suspend fun setGameTurnProcessing(isProcessing: Boolean) {
        setGameProperties(null, isProcessing)
    }

    private suspend fun incrementGameTurn() {
        val game = entityController
            .findByIds(stateStore.findKeysByType("Game"))
            .firstNotNullOf { it.value } as Game

        // TEMPORARY DEBUG
        log.info("TURN_LOOP: Got game with id ${game._id}")
        // TEMPORARY DEBUG
        log.info("TURN_LOOP: Incrementing turn")

        val properties = JsonObject()
            .put("currentTurn", game.currentTurn + 1)

        entityController.saveProperties(game._id!!, properties)
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