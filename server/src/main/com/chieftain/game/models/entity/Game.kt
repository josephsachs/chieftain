package chieftain.game.models.entity

import chieftain.game.action.GameTaskHandler
import chieftain.game.action.GameTurnHandler
import com.chieftain.game.scenario.GameState
import com.google.inject.Inject
import com.minare.core.entity.annotations.EntityType
import com.minare.core.entity.annotations.FixedTask
import com.minare.core.entity.annotations.Task
import com.minare.core.entity.models.Entity
import org.slf4j.LoggerFactory

@EntityType("Game")
class Game: Entity() {
    @Inject private lateinit var gameState: GameState
    @Inject private lateinit var gameTaskHandler: GameTaskHandler
    @Inject private lateinit var gameTurnHandler: GameTurnHandler

    init {
        type = "Game"
    }

    private val name: String = "MyGame"

    @Task
    suspend fun task() {
        if (gameState.isGamePaused()) return

        gameTaskHandler.handle()

        // Check if turn
        // then turn handle
    }

    @FixedTask
    suspend fun fixedTask() {
        if (gameState.isGamePaused()) return

        gameTurnHandler.handle()
    }
}