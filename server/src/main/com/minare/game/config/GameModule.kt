package com.minare.game.config

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
import com.minare.game.GameEntityFactory
import com.minare.game.controller.GameChannelController
import com.minare.game.controller.GameConnectionController
import com.minare.game.controller.GameMessageController
import com.minare.game.controller.GameOperationController
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
        bind(EntityFactory::class.java).annotatedWith(Names.named("userEntityFactory"))
            .to(GameEntityFactory::class.java).`in`(Singleton::class.java)

        bind(ChannelController::class.java).to(GameChannelController::class.java).`in`(Singleton::class.java)
        bind(ConnectionController::class.java).to(GameConnectionController::class.java).`in`(Singleton::class.java)
        bind(OperationController::class.java).to(GameOperationController::class.java).`in`(Singleton::class.java)
        bind(MessageController::class.java).to(GameMessageController::class.java).`in`(Singleton::class.java)

        // Expose the named user factory (framework will wrap it)
        expose(EntityFactory::class.java).annotatedWith(Names.named("userEntityFactory"))
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

    override fun getDatabaseName(): String = "node_graph"
}