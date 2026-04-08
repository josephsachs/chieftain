package chieftain.game.models.entity

import chieftain.game.models.data.Vector2
import com.chieftain.game.models.data.Depot
import com.chieftain.game.models.entity.Culture
import com.minare.core.entity.annotations.EntityType
import com.minare.core.entity.annotations.Mutable
import com.minare.core.entity.annotations.Parent
import com.minare.core.entity.annotations.State
import com.minare.core.entity.models.Entity
import java.io.Serializable

@EntityType("City")
class City: Entity() {
    init {
        type = "City"
    }

    @State
    var name: String = ""

    @State
    var location: Vector2 = Vector2(0, 0)

    @State
    @Mutable
    var population: Int = 0

    @State
    @Mutable
    var culture: Culture.Companion.CultureGroup = Culture.Companion.CultureGroup.UNASSIGNED

    @State
    @Mutable
    @Parent
    var alignedWith: Polity? = null

    @State
    @Mutable
    var market: Depot = Depot()

    @State
    var exchangeRates: ExchangeRates = ExchangeRates()

    companion object {
        /**
         * Exchange rates define what a city buys (and at what multiplier)
         * and what food it sells (and at what price in wealth units).
         * A buyRate of 3 for GOLD means 1 gold → 3 wealth units.
         * A sellRate of 2 for CORN means 2 wealth units → 1 corn.
         */
        data class ExchangeRates(
            val buyRates: Map<String, Int> = mapOf(
                "WOOD" to 1,
                "PAPYRUS" to 1,
                "STONE" to 1,
                "IRON" to 2,
                "COPPER" to 2,
                "TIN" to 2,
                "GOLD" to 3,
                "JEWELS" to 3,
                "STATUES" to 2,
                "COINS" to 1
            ),
            val sellRates: Map<String, Int> = mapOf(
                "CORN" to 1,
                "FRUIT" to 2,
                "FOWL" to 2,
                "MEAT" to 3,
                "HONEY" to 2
            )
        ) : Serializable
    }
}