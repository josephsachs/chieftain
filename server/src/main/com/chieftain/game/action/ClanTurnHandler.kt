package chieftain.game.action

import chieftain.game.models.entity.agent.Clan
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.controller.OperationController
import com.minare.core.storage.interfaces.StateStore
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject

@Singleton
class ClanTurnHandler @Inject constructor(
    private val entityController: EntityController,
    private val stateStore: StateStore,
) {
    suspend fun handleTurn(turnPhase: GameTurnHandler.Companion.TurnPhase): JsonObject {
        val clans = entityController.findByIds(
            stateStore.findAllKeysForType("Clan")
        )

        var dataResponse = JsonObject()

        for ((key, clan) in clans) {
            clan as Clan
            when (turnPhase) {
                GameTurnHandler.Companion.TurnPhase.ACT -> {
                    dataResponse.put(clan.name, doAct(clan))
                }
                GameTurnHandler.Companion.TurnPhase.EXECUTE -> {
                    dataResponse.put(clan.name, doExecute(clan))
                }
                GameTurnHandler.Companion.TurnPhase.RESOLVE -> {
                    dataResponse.put(clan.name, doResolve(clan))
                }
            }
        }

        return dataResponse
    }

    suspend fun doAct(clan: Clan): JsonObject {
        var dataResponse = JsonObject()

        clan.chooseBehavior()

        dataResponse.mergeIn(JsonObject()
            .put("clanName", clan.name)
            .put("clanBehavior", clan.behavior.toString())
        )

        when (clan.behavior) {
            Clan.Companion.ClanBehavior.NONE -> {
                // Nothing
            }
            Clan.Companion.ClanBehavior.WANDERING -> {
                // Nothing
            }
            Clan.Companion.ClanBehavior.TRAVELING -> {
                // Reverify our route, set intention
            }
            Clan.Companion.ClanBehavior.LABORING -> {
                // Nothing
            }
            Clan.Companion.ClanBehavior.HOLIDAY -> {
                //clan.holidayBehavior()
            }
            Clan.Companion.ClanBehavior.FIGHTING -> {
                //var target = clan.tryGetTarget()
                //if (gameMapController.isAdjacent(clan, target)) {
                //    set our intention to attack
                //} else {
                //    set our intention to move toward
                //}
            }
            Clan.Companion.ClanBehavior.RECOVERING -> {
                //var target = clan.tryGetTarget()
                //if (gameMapController.isAdjacent(clan, target)) {
                //    set our intention to move away
                //} else {
                // // do nothing for now...
                //}
            }
            else -> {
                throw IllegalStateException("TURN_LOOP: ClanTurnHandler found clan ${clan._id} with undefined behavior ${clan.behavior}")
            }
        }

        return dataResponse
    }

    suspend fun doExecute(clan: Clan): JsonObject {
        var dataResponse = JsonObject()

        dataResponse.mergeIn(JsonObject()
            .put("clanName", clan.name)
            .put("clanBehavior", clan.behavior.toString())
        )

        when (clan.behavior) {
            Clan.Companion.ClanBehavior.NONE -> {
                // Nothing
            }
            Clan.Companion.ClanBehavior.WANDERING -> {
                clan.queueWanderAction()
            }
            Clan.Companion.ClanBehavior.TRAVELING -> {
                //Find our way toward the nav target one hex at a time
            }
            Clan.Companion.ClanBehavior.LABORING -> {
                //clan.queueLaborAction(clan.targetResource)
            }
            Clan.Companion.ClanBehavior.HOLIDAY -> {
                //clan.holidayBehavior()
            }
            Clan.Companion.ClanBehavior.FIGHTING -> {
                //clan.queueAttack()
                // or
                //clan.queueMove()
            }
            Clan.Companion.ClanBehavior.RECOVERING -> {
                //clan.queueMove()
            }
            else -> {
                throw IllegalStateException("TURN_LOOP: ClanTurnHandler found clan ${clan._id} with undefined behavior ${clan.behavior}")
            }
        }

        return dataResponse
    }

    suspend fun doResolve(clan: Clan): JsonObject {
        var dataResponse = JsonObject()

        dataResponse.mergeIn(JsonObject()
            .put("clanName", clan.name)
            .put("clanBehavior", clan.behavior.toString())
        )

        clan.dynamics()

        return dataResponse
    }
}