package chieftain.game.models.data

class MapFeature {
    var location: Pair<Int, Int> = Pair(0, 0)

    companion object {
        enum class MapFeatureType {
            HARBOR,
            RIVER,
            OASIS,
            TOWN
        }
    }
}