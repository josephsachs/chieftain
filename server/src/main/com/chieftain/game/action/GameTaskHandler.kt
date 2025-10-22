package chieftain.game.action

import chieftain.game.action.cache.MapDataCache
import chieftain.game.models.entity.agent.Clan
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.core.storage.interfaces.StateStore
import com.minare.core.utils.vertx.EventBusUtils
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject

@Singleton
class GameTaskHandler @Inject constructor(
    private val mapDataCache: MapDataCache,
    private val eventBusUtils: EventBusUtils
) {
    private var log = LoggerFactory.getLogger(GameTaskHandler::class.java)

    suspend fun handle() {
        try {
            log.info("TURN_LOOP: Game tasks handling...")
        } finally {
            eventBusUtils.publishWithTracing(
                ADDRESS_GAME_TASKS_COMPLETE,
                JsonObject()
            )
        }
    }

    companion object {
        const val ADDRESS_GAME_TASKS_COMPLETE = "turn.handler.game.tasks.complete"
    }
}