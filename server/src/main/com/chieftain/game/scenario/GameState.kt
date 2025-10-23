package com.chieftain.game.scenario

import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.core.utils.PushVar
import org.slf4j.LoggerFactory

@Singleton
class GameState @Inject constructor(
    private val pushVar: PushVar.Factory
) {
    private val log = LoggerFactory.getLogger(GameState::class.java)

    private val _gameClockState = pushVar.create(
        address = "game.clock.state",
        initialValue = GameClockState.PAUSED,
        serializer = { it.name },  // Serialize enum to string
        deserializer = { GameClockState.valueOf(it as String) }  // Deserialize string back to enum
    )

    fun isGamePaused(): Boolean {
        return _gameClockState.get() == GameClockState.PAUSED
    }

    fun pauseGameClock() {
        _gameClockState.set(GameClockState.PAUSED)
    }

    fun resumeGameClock() {
        _gameClockState.set(GameClockState.RUNNING)
    }

    companion object {
        enum class GameClockState {
            RUNNING,
            PAUSED
        }
    }
}