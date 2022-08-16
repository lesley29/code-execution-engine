package com.example

import com.example.data.MongoContext
import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
import io.ktor.server.application.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureRouting()
    configureSerialization()

    install(Koin) {
        slf4jLogger()
        installDependencies(environment)
    }
}

fun KoinApplication.installDependencies(environment: ApplicationEnvironment) {
    val connectionString = environment.config.property("connectionStrings.mongodb").getString()

    val dependencies = module {
        single { MongoContext(connectionString) }
    }

    modules(dependencies)
}

