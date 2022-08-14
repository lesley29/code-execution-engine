package com.example

import com.example.data.MongoContext
import com.sksamuel.hoplite.ConfigLoader
import kotlinx.coroutines.*
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module
import sun.misc.Signal

fun main(args: Array<String>) = runBlocking<Unit> {
    Signal.handle(Signal("INT")) {
        this.coroutineContext.cancelChildren()
    }

    val config = ConfigLoader().loadConfigOrThrow<Config>("./config.yaml")

    startKoin {
        installDependencies(config)
    }

    launch {
        Job().execute()
    }
}

fun KoinApplication.installDependencies(config: Config) {
    val connectionString = config.connectionStrings.mongodb

    val dependencies = module {
        single { MongoContext(connectionString) }
    }

    modules(dependencies)
}