package chieftain.game.action

import chieftain.game.models.entity.agent.Clan
import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.controller.OperationController
import io.vertx.core.impl.logging.LoggerFactory

@Singleton
class ClanTurnHandler @Inject constructor(
    private val operationController: OperationController
) {
    private val log = LoggerFactory.getLogger(ClanTurnHandler::class.java)

    suspend fun handleTask(clan: Clan) {
        when (clan.behavior) {
            Clan.Companion.ClanBehavior.NONE -> {
                // Nothing
            }
            Clan.Companion.ClanBehavior.WANDERING -> {
                // Pick a legal direction and move
                log.info("Clan ${clan.name} decides to wander")
            }
            else -> {
                throw IllegalStateException("ClanTurnHandler found clan ${clan._id} with undefined behavior ${clan.behavior}")
            }
        }
    }

    suspend fun handleTurn(clan: Clan) {
        when (clan.behavior) {
            Clan.Companion.ClanBehavior.NONE -> {
                // Nothing
            }
            Clan.Companion.ClanBehavior.WANDERING -> {
                // Pick a legal direction and move
                log.info("Clan ${clan.name }is wandering")
            }
            else -> {
                throw IllegalStateException("ClanTurnHandler found clan ${clan._id} with undefined behavior ${clan.behavior}")
            }
        }
    }
}