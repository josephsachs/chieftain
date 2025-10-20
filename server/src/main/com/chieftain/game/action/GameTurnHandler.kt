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
class GameTurnHandler @Inject constructor(
    private val mapDataCache: MapDataCache,
    private val clanTurnHandler: ClanTurnHandler,
    private val entityController: EntityController,
    private val stateStore: StateStore,
    private val eventBusUtils: EventBusUtils
) {
    private var log = LoggerFactory.getLogger(GameTurnHandler::class.java)

    suspend fun handle() {
        val clans = entityController.findByIds(
            stateStore.findKeysByType("Clan")
        )

        log.info("clans: ${clans.size}")

        try {
            for ((key, clan) in clans) {
                log.info("clan: ${key}")

                clanTurnHandler.handleTurn(clan as Clan)
            }
        } finally {
            eventBusUtils.publishWithTracing(
                ADDRESS_TURN_COMPLETE,
                JsonObject()
                    .put("Clans", clans.size)
            )
        }
    }

    companion object {
        const val ADDRESS_TURN_COMPLETE = "turn.handler.fixed.task.complete"
    }
}