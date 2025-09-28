package com.chieftain.game.scenario

import com.chieftain.game.controller.GameChannelController
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.json.JsonObject

@Singleton
class AgentInitializer @Inject constructor(
    private val gameChannelController: GameChannelController
) {
    suspend fun initialize(agentData: JsonObject) {

    }
}