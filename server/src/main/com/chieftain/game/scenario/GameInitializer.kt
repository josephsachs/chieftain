package com.chieftain.game.scenario

import chieftain.game.models.entity.Game
import com.chieftain.game.GameEntityFactory
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.core.utils.vertx.VerticleLogger
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

@Singleton
class GameInitializer @Inject constructor(
    private val mapInitializer: MapInitializer,
    private val agentInitializer: AgentInitializer,
    private val gameState: GameState,
    private val entityController: EntityController,
    private val entityFactory: GameEntityFactory,
    private val vertx: Vertx,
    private val verticleLogger: VerticleLogger
) {
    suspend fun initialize() {
        verticleLogger.logInfo("Chieftain: Initializing map")

        entityController.create(
            entityFactory.createEntity(Game::class.java)
        )

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