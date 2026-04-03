package com.chieftain.game

import chieftain.game.models.entity.Deity
import chieftain.game.models.entity.Game
import chieftain.game.models.entity.Polity
import chieftain.game.models.entity.agent.Clan
import chieftain.game.models.entity.agent.Fight
import chieftain.game.models.entity.agent.Treaty
import chieftain.game.models.entity.mapfeature.City
import com.chieftain.game.models.entity.Culture
import com.minare.core.entity.factories.EntityFactory
import com.chieftain.game.models.entity.MapZone
import com.chieftain.game.models.entity.mapfeature.MapFeature
import com.google.inject.Inject
import javax.inject.Singleton

/**
 * Game EntityFactory implementation.
 * Updated to remove dependency injection since Entity is now a pure data class.
 */
@Singleton
class GameEntityFactory @Inject constructor(): EntityFactory() {
    // Just define the map - framework does the rest!
    override val entityTypes = mapOf(
        "Game" to Game::class.java,
        "MapZone" to MapZone::class.java,
        "MapFeature" to MapFeature::class.java,
        "City" to City::class.java,
        "Clan" to Clan::class.java,
        "Culture" to Culture::class.java,
        "Treaty" to Treaty::class.java,
        "Fight" to Fight::class.java,
        "Polity" to Polity::class.java
    )
}