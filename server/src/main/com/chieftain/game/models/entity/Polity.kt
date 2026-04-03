package chieftain.game.models.entity

import chieftain.game.models.entity.agent.Character
import chieftain.game.models.entity.agent.Treaty
import com.chieftain.game.models.entity.Culture
import com.minare.core.entity.annotations.*
import com.minare.core.entity.models.Entity

@EntityType("polity")
class Polity(): Entity() {
    @State
    @Mutable
    var name: String = ""

    @State
    @Mutable
    var leader: Character = Character()

    @State
    @Mutable
    var deity: Deity = Deity(
        "unnamed",
        Culture.Companion.CultureGroup.UNASSIGNED,
        mutableSetOf()
    )

    @State
    @Mutable
    var members: MutableMap<PolityMemberType, List<String>> = mutableMapOf(
        PolityMemberType.UNASSIGNED to mutableListOf(),
        PolityMemberType.KING to mutableListOf(),
        PolityMemberType.CITY to mutableListOf(),
        PolityMemberType.CLAN to mutableListOf(),
        PolityMemberType.PRIESTHOOD to mutableListOf()
    )

    @State
    @Mutable
    @Parent
    var subjectTo: Polity? = null

    @State
    @Mutable
    @Child
    var hasSubjects: MutableSet<Polity> = mutableSetOf()

    @State
    @Mutable
    var hasTreaties: MutableList<Treaty> = mutableListOf()
}

enum class PolityMemberType(value: String) {
    UNASSIGNED("Unassigned"),
    KING("King"),
    CITY("City"),
    CLAN("Clan"),
    PRIESTHOOD("Priesthood")
}