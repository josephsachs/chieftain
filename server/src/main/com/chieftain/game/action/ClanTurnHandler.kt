package chieftain.game.action

import chieftain.game.models.entity.agent.Character
import chieftain.game.models.entity.agent.Clan
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.EntityController
import com.minare.core.storage.interfaces.StateStore
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

        // Hydrate chieftain references
        val chieftainIds = clans.values
            .filterIsInstance<Clan>()
            .map { it.chieftainId }
            .filter { it.isNotEmpty() }
            .distinct()
        val characters = if (chieftainIds.isNotEmpty()) {
            entityController.findByIds(chieftainIds)
        } else emptyMap()

        val dataResponse = JsonObject()

        for ((_, clan) in clans) {
            clan as Clan
            clan.chieftain = characters[clan.chieftainId] as? Character
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

    private suspend fun doAct(clan: Clan): JsonObject {
        val dataResponse = JsonObject()

        clan.chooseBehavior()

        dataResponse.mergeIn(JsonObject()
            .put("clanName", clan.name)
            .put("clanBehavior", clan.behavior.toString())
        )

        // ACT phase: decisions and intentions are set inside chooseBehavior.
        // Behavior-specific act-phase logic goes here as combat/diplomacy systems grow.

        return dataResponse
    }

    private suspend fun doExecute(clan: Clan): JsonObject {
        val dataResponse = JsonObject()
            .put("clanName", clan.name)
            .put("clanBehavior", clan.behavior.toString())

        when (clan.behavior) {
            Clan.Companion.ClanBehavior.NONE -> {}
            Clan.Companion.ClanBehavior.WANDERING -> {
                clan.queueWanderAction()
            }
            Clan.Companion.ClanBehavior.TRAVELING -> {
                clan.queueTravelAction()
            }
            Clan.Companion.ClanBehavior.LABORING -> {
                clan.queueLaborAction()
            }
            Clan.Companion.ClanBehavior.HOLIDAY -> {
                // Rest: stamina recovery happens in dynamics
            }
            Clan.Companion.ClanBehavior.FIGHTING -> {
                // TODO: clan.queueCombatAction()
            }
            Clan.Companion.ClanBehavior.RECOVERING -> {
                // TODO: clan.queueRetreatAction()
            }
        }

        return dataResponse
    }

    private suspend fun doResolve(clan: Clan): JsonObject {
        val dataResponse = JsonObject()
            .put("clanName", clan.name)
            .put("clanBehavior", clan.behavior.toString())

        clan.recordLocationMemory()
        clan.dynamics()

        return dataResponse
    }
}