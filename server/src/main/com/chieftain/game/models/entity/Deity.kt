package chieftain.game.models.entity

import com.chieftain.game.models.entity.Culture
import java.io.Serializable

data class Deity(
    val name: String,
    val culture: Culture.Companion.CultureGroup,
    val ideology: MutableSet<DeityIdeology>
): Serializable

enum class DeityIdeology(value: String) {
    COSMIC_RENEWAL("Cosmic Renewal"),
    ALL_CREATOR("All-Creator"),
    DYNASTIC("Dynastic"),
    PATRIMONIAL("Patrimonial"),
    APOCALYPTIC("Apocalyptic"),
    PERSONAL_PIETY("Personal Piety"),
    JEALOUS("Jealous"),
    MOUNTAIN_BAAL("Mountain Baal"),
    STORMS("Storms"),
    WARLIKE("Warlike"),
    MAGICIAN("Magician")
}