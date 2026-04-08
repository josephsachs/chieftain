package chieftain.game.models.entity.agent

import java.io.Serializable

data class CombatUnit(
    var forces: Int = 0,
    var quality: Int = 0,
    var weaponAtt: Int = 0,
    var armorDef: Int = 0,
    var morale: Int = 100,
    var mobility: Int = 0
): Serializable