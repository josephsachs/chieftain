package com.chieftain.game.scenario

import com.google.inject.Inject
import com.google.inject.Singleton
import com.minare.core.frames.worker.FrameWorkerVerticle.Companion.ADDRESS_NEXT_FRAME
import com.minare.core.utils.PushVar
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

@Singleton
class GameState @Inject constructor(
    private val pushVarFactory: PushVar.Factory,
    private val vertx: Vertx
) {
    private val log = LoggerFactory.getLogger(GameState::class.java)

    private val eventBus = vertx.eventBus()

    private var currentFrame: Long = 0L
    private var currentTask: Long = 0L

    private val _gameClockState = pushVarFactory.create(
        address = "game.clock.state",
        initialValue = GameClockState.PAUSED,
        serializer = { it.name },  // Serialize enum to string
        deserializer = { GameClockState.valueOf(it as String) }  // Deserialize string back to enum
    )

    private var _gameYear: Int = 1400
    private var _gameDay: Int = 1

    private var dateString: String = "Year $_gameYear, day $_gameDay"

    fun getGameDate(): String {
        return dateString
    }

    fun isGamePaused(): Boolean {
        return _gameClockState.get() == GameClockState.PAUSED
    }

    fun pauseGameClock() {
        log.info("TURN_CONTROLLER: Game state is now paused")
        _gameClockState.set(GameClockState.PAUSED)
    }

    fun resumeGameClock() {
        log.info("TURN_CONTROLLER: Game state is now running")
        _gameClockState.set(GameClockState.RUNNING)
    }

    fun incrementDay() {
        _gameDay++

        if (_gameDay > 365) {
            incrementYear()
            _gameDay = 1
        }
    }

    private fun incrementYear() {
        _gameYear--

        if (_gameYear < 0) {
            log.info("Game lasted 1400 years, should prob be over by now")
        }
    }

    companion object {
        enum class GameClockState {
            RUNNING,
            PAUSED
        }
    }
}