package chieftain.game.models.entity.agent

import chieftain.game.models.data.Vector2
import chieftain.game.models.entity.Polity
import com.chieftain.game.models.entity.Culture
import com.minare.core.entity.annotations.*
import com.minare.core.entity.models.Entity
import java.io.Serializable

@EntityType("Character")
class Character(): Entity(), Agent {
    init {
        type = "Character"
    }

    @State
    var name: String = ""

    @State
    @Mutable
    var culture: Culture.Companion.CultureGroup = Culture.Companion.CultureGroup.UNASSIGNED

    @State
    @Mutable
    var title: CharacterTitle = CharacterTitle.NONE

    @State
    @Mutable
    var stats: CharacterStats = CharacterStats()

    @State
    @Mutable
    var personality: CharacterPersonality = CharacterPersonality()

    @State
    @Mutable
    var traits: MutableSet<CharacterTraits> = mutableSetOf()

    @State
    @Mutable
    @Parent
    var polity: Polity = Polity()

    @State
    @Mutable
    var mapLocation: Vector2 = Vector2(0,0)

    companion object {
        data class CharacterStats(
            var speech: Int = 0,
            val peacekeeping: Int = 0,
            var fighting: Int = 0,
            var pathfinding: Int = 0,
            var trading: Int = 0,
            var overseeing: Int = 0,
            var scouting: Int = 0,
            var intrigue: Int = 0,
            var mysticism: Int = 0,
            var erudition: Int = 0
        ): Serializable

        data class CharacterPersonality(
            // 0.00 < n < 1.00
            var cooperatorVsDefector: Double = 0.00,
            var lawfulVsChaotic: Double = 0.00,
            var grandioseVsInsecure: Double = 0.00,
            var riskyVsCautious: Double = 0.00,
            var ethicalVsAmoral: Double = 0.00,
            var sumptuousVsPrudent: Double = 0.00
        ): Serializable
        
        enum class CharacterTitle(value: String) {
            NONE("None"),
            CHIEFTAIN("Chieftain"),
            PRINCE("Prince"),
            GOVERNOR("Governor"),
            GENERAL("General")
        }

        enum class CharacterTraits(value: String) {
            RESTLESS("Restless"),
            GREEDY("Greedy"),
            COMPASSIONATE("Compassionate"),
            CALLOUS("Callous"),
            LAWGIVER("Lawgiver"),
            ARBITRARY("Arbitrary"),
            LITERATE("Literate"),
            STRONG("Strong")
        }
    }
}