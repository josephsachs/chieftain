package chieftain.game.action

import chieftain.game.models.entity.agent.Fight
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.core.storage.interfaces.StateStore
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject

@Singleton
class FightTurnHandler @Inject constructor(
    private val entityController: EntityController,
    private val stateStore: StateStore,
) {
    private val log = LoggerFactory.getLogger(ClanTurnHandler::class.java)

    suspend fun handleTurn(turnPhase: GameTurnHandler.Companion.TurnPhase): JsonObject {
        val fights = entityController.findByIds(
            stateStore.findAllKeysForType("Fight")
        )

        var dataResponse = JsonObject()

        for ((key, fight) in fights) {
            fight as Fight
            when (turnPhase) {
                GameTurnHandler.Companion.TurnPhase.ACT -> {
                    dataResponse.put(fight.label, doAct(fight))
                }
                GameTurnHandler.Companion.TurnPhase.EXECUTE -> {
                    dataResponse.put(fight.label, doExecute(fight))
                }
                GameTurnHandler.Companion.TurnPhase.RESOLVE -> {
                    dataResponse.put(fight.label, doAct(fight))
                }
            }
        }

        return dataResponse
    }

    suspend fun doAct(fight: Fight): JsonObject {
        var dataResponse = JsonObject()



        return dataResponse
    }

    suspend fun doExecute(fight: Fight): JsonObject {
        var dataResponse = JsonObject()



        return dataResponse
    }

    suspend fun doResolve(fight: Fight): JsonObject {
        var dataResponse = JsonObject()



        return dataResponse
    }
}