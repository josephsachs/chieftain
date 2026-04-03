package chieftain.game.action

import chieftain.game.models.entity.agent.Clan
import com.chieftain.game.models.entity.MapZone
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.controller.OperationController
import com.minare.core.storage.interfaces.StateStore
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject

@Singleton
class MapZoneTurnHandler @Inject constructor(
    private val entityController: EntityController,
    private val stateStore: StateStore,
) {
    private val log = LoggerFactory.getLogger(MapZoneTurnHandler::class.java)

    suspend fun handleTurn(turnPhase: GameTurnHandler.Companion.TurnPhase): JsonObject {
        val mapZones = entityController.findByIds(
            stateStore.findAllKeysForType("MapZone")
        )
        log.info("TURN_LOOP: Got here 3 clans $mapZones")

        var dataResponse = JsonObject()

        for ((key, mz) in mapZones) {
            mz as MapZone
            when (turnPhase) {
                GameTurnHandler.Companion.TurnPhase.ACT -> {
                    dataResponse.put("${mz.location.x},${mz.location.y}", doAct(mz))
                }
                GameTurnHandler.Companion.TurnPhase.EXECUTE -> {

                }
                GameTurnHandler.Companion.TurnPhase.RESOLVE -> {
                    dataResponse.put("${mz.location.x},${mz.location.y}", doResolve(mz))
                }
            }
        }

        return dataResponse
    }

    private suspend fun doAct(mz: MapZone): JsonObject {
        val data = JsonObject()

        // if ( we have decided to do that this round )
        // {
        //      choose candidates for events and deal
        // }

        return data
    }

    private suspend fun doResolve(mz: MapZone): JsonObject {
        val data = JsonObject()

        // if ( we have decided to do that this round )
        // {
        //      spawn resources
        //      maintain boundary conditions
        // }

        return data
    }
}