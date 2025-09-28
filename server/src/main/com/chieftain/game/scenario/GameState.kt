package com.chieftain.game.scenario

import com.google.inject.Inject
import com.google.inject.Singleton
import com.hazelcast.core.HazelcastInstance
import com.sun.jdi.IntegerValue
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

@Singleton
class GameState @Inject constructor(
    private val hazelcastInstance: HazelcastInstance
) {
    private val log = LoggerFactory.getLogger(GameState::class.java)

    private var gameClockState: GameClockState = GameClockState.PAUSED
    private var _gameYear: Int = 1400
    private var _gameDay: Int = 1

    private var dateString: String = "Year $_gameYear, day $_gameDay"

    fun getGameDate(): String {
        return dateString
    }

    fun pauseGameClock() {
        gameClockState = GameClockState.PAUSED
    }

    fun resumeGameClock() {
        gameClockState = GameClockState.RUNNING
    }

    private fun incrementDay() {
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