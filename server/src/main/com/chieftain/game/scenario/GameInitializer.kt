package com.chieftain.game.scenario

import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.core.utils.vertx.VerticleLogger
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

@Singleton
class GameInitializer @Inject constructor(
    private val mapInitializer: MapInitializer,
    private val agentInitializer: AgentInitializer,
    private val gameState: GameState,
    private val vertx: Vertx,
    private val verticleLogger: VerticleLogger
) {
    suspend fun initialize() {
        verticleLogger.logInfo("Chieftain: Initializing map")

        mapInitializer.initialize()
        agentInitializer.initialize()

        vertx.eventBus().publish(ADDRESS_INITIALIZE_GAME_COMPLETE, JsonObject())

        verticleLogger.logInfo("Chieftain: Game initialized")

        gameState.resumeGameClock()
    }

    companion object {
        const val ADDRESS_INITIALIZE_GAME_COMPLETE = "chieftain.initialize.game.complete"
    }
}