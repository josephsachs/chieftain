package com.chieftain.game.models.data

class Depot {
    var contents: Map<ResourceTypeGroup, Map<ResourceType, Int>> = mapOf()

    init {
        contents = mapOf(
            ResourceTypeGroup.FOOD to mapOf(
                ResourceType.CORN to 0,
                ResourceType.FOWL to 0,
                ResourceType.MEAT to 0,
                ResourceType.FRUIT to 0,
                ResourceType.HONEY to 0
            ),
            ResourceTypeGroup.GOODS to mapOf(
                ResourceType.WOOD to 0,
                ResourceType.PAPYRUS to 0,
                ResourceType.STONE to 0
            ),
            ResourceTypeGroup.METALS to mapOf(
                ResourceType.IRON to 0,
                ResourceType.COPPER to 0,
                ResourceType.TIN to 0,
                ResourceType.GOLD to 0
            ),
            ResourceTypeGroup.TREASURE to mapOf(
                ResourceType.BOOKS to 0,
                ResourceType.JEWELS to 0,
                ResourceType.STATUES to 0,
                ResourceType.COINS to 0
            )
        )
    }

    companion object {
        enum class ResourceTypeGroup {
            FOOD,
            GOODS,
            METALS,
            TREASURE
        }

        enum class ResourceType {
            CORN,
            FOWL,
            MEAT,
            FRUIT,
            HONEY,
            WOOD,
            PAPYRUS,
            STONE,
            IRON,
            COPPER,
            TIN,
            GOLD,
            BOOKS,
            JEWELS,
            STATUES,
            COINS
        }
    }
}