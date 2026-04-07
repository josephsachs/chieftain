package chieftain.game.models.entity

import chieftain.game.models.entity.agent.CombatUnit

interface Combatant {
    val _id: String
    var combatUnit: CombatUnit
}