package com.minare.game

import com.minare.Game.GameApplication
import com.minare.core.MinareApplication
import org.slf4j.LoggerFactory

/**
 * Main entry point for running the Game application
 */
object GameMain {
    private val log = LoggerFactory.getLogger(GameMain::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("Starting Minare Game Application")


        MinareApplication.start(GameApplication::class.java, args)
    }
}