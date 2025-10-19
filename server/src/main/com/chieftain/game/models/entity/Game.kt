package chieftain.game.models.entity

import chieftain.game.action.GameTaskHandler
import com.chieftain.game.scenario.GameState
import com.google.inject.Inject
import com.minare.core.entity.annotations.EntityType
import com.minare.core.entity.annotations.Task
import com.minare.core.entity.models.Entity
import org.slf4j.LoggerFactory

@EntityType("Game")
class Game: Entity() {
    @Inject private lateinit var gameState: GameState
    @Inject private lateinit var gameTaskHandler: GameTaskHandler

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
}