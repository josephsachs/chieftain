package com.chieftain.game

import chieftain.game.models.entity.Game
import chieftain.game.models.entity.Polity
import chieftain.game.models.entity.agent.Character
import chieftain.game.models.entity.agent.Clan
import chieftain.game.models.entity.agent.Fight
import chieftain.game.models.entity.agent.Treaty
import chieftain.game.models.entity.City
import com.chieftain.game.models.entity.Culture
import com.chieftain.game.models.entity.MapZone
import com.google.inject.Inject
import com.minare.core.entity.factories.EntityFactory
import javax.inject.Singleton

@Singleton
class GameEntityFactory @Inject constructor(): EntityFactory() {
    override val entityTypes = mapOf(
        "Game" to Game::class.java,
        "MapZone" to MapZone::class.java,
        "City" to City::class.java,
        "Character" to Character::class.java,
        "Clan" to Clan::class.java,
        "Culture" to Culture::class.java,
        "Treaty" to Treaty::class.java,
        "Fight" to Fight::class.java,
        "Polity" to Polity::class.java
    )
}