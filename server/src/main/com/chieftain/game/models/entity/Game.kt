package chieftain.game.models.entity

import chieftain.game.action.GameTaskHandler
import chieftain.game.action.GameTurnHandler
import chieftain.game.action.GameTurnHandler.Companion.TurnPhase
import com.chieftain.game.scenario.GameState
import com.google.inject.Inject
import com.minare.controller.EntityController
import com.minare.core.entity.annotations.*
import com.minare.core.entity.models.Entity
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

@EntityType("Game")
class Game: Entity() {
    @Inject private lateinit var gameState: GameState
    @Inject private lateinit var gameTaskHandler: GameTaskHandler
    @Inject private lateinit var gameTurnHandler: GameTurnHandler

    private val log = LoggerFactory.getLogger(Game::class.java)

    init {
        type = "Game"
    }

    private val name: String = "MyGame"

    @Property
    var currentTurn: Int = 0

    @Property
    var turnPhase: TurnPhase = TurnPhase.ACT

    @Property
    var turnProcessing: Boolean = false

    @Task
    suspend fun task() {
        if (gameState.isGamePaused()) return

        log.info("TURN_LOOP: Game task function found phase ${turnPhase}")

        gameTaskHandler.handle()
    }

    @FixedTask
    suspend fun fixedTask() {
        log.info("TURN_LOOP: Started fixed task found phase ${turnPhase}")
        if (gameState.isGamePaused()) return

        log.info("TURN_LOOP: Processing...")
        gameTurnHandler.handleFrame(this)
    }
}