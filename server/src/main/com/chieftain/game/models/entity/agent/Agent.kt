package chieftain.game.models.entity.agent

abstract class Agent {

    companion object {
        enum class SkillType {
            HUNTING,
            GATHERING,
            MINING,
            QUARRYING,
            WOODCUTTING,
            SCULPTURE,
            JEWELRY,
            SCRIBING,
            MINTING
        }
    }
}