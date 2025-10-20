package com.chieftain.game.config

import com.chieftain.application.config.GameFrameConfiguration
import com.google.inject.PrivateModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Names
import com.minare.controller.ChannelController
import com.minare.controller.ConnectionController
import com.minare.controller.MessageController
import com.minare.controller.OperationController
import com.minare.core.config.DatabaseNameProvider
import com.minare.core.entity.factories.EntityFactory
import com.chieftain.game.GameEntityFactory
import com.chieftain.game.controller.GameChannelController
import com.chieftain.game.controller.GameConnectionController
import com.chieftain.game.controller.GameMessageController
import com.chieftain.game.controller.GameOperationController
import com.minare.application.config.FrameConfiguration
import com.minare.application.config.GameTaskConfiguration
import com.minare.application.config.TaskConfiguration
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

/**
 * Application-specific Guice module for the Game app.
 * This provides bindings specific to our Game application.
 *
 * When combined with the framework through a child injector,
 * bindings defined here will override the framework's default bindings.
 */
class GameModule : PrivateModule(), DatabaseNameProvider {
    private val log = LoggerFactory.getLogger(GameModule::class.java)

    override fun configure() {
        bind(EntityFactory::class.java)
            .annotatedWith(Names.named("user"))
            .to(GameEntityFactory::class.java)
        bind(FrameConfiguration::class.java).to(GameFrameConfiguration::class.java).`in`(Singleton::class.java)
        bind(TaskConfiguration::class.java).to(GameTaskConfiguration::class.java).`in`(Singleton::class.java)

        bind(ChannelController::class.java).to(GameChannelController::class.java).`in`(Singleton::class.java)
        bind(ConnectionController::class.java).to(GameConnectionController::class.java).`in`(Singleton::class.java)
        bind(OperationController::class.java).to(GameOperationController::class.java).`in`(Singleton::class.java)
        bind(MessageController::class.java).to(GameMessageController::class.java).`in`(Singleton::class.java)

        // Expose the named user factory (framework will wrap it)
        expose(EntityFactory::class.java).annotatedWith(Names.named("user"))
        expose(FrameConfiguration::class.java)
        expose(TaskConfiguration::class.java)
        expose(ChannelController::class.java)
        expose(ConnectionController::class.java)
        expose(OperationController::class.java)
        expose(MessageController::class.java)

        log.info("GameModule configured with custom EntityFactory and controllers")
    }

    @Provides
    @Singleton
    fun provideCoroutineScope(coroutineContext: CoroutineContext): CoroutineScope {
        return CoroutineScope(coroutineContext)
    }

    override fun getDatabaseName(): String = "chieftain_game"
}