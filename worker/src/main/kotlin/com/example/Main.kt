package com.example

import com.example.data.MongoContext
import com.example.model.Task
import com.sksamuel.hoplite.ConfigLoader
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import sun.misc.Signal
import java.util.*

fun main(args: Array<String>) = runBlocking<Unit> {
    Signal.handle(Signal("INT")) {
        this.coroutineContext.cancelChildren()
        stopKoin()
    }

    val config = ConfigLoader().loadConfigOrThrow<Config>("./config.yaml")
    val consumerProperties = javaClass.classLoader.getResourceAsStream("consumer.properties").use {
        Properties().apply {
            load(it)
        }
    }

    startKoin {
        installDependencies(config, consumerProperties)
    }

    launch {
        Worker().run()
    }
}

fun KoinApplication.installDependencies(config: Config, consumerProperties: Properties) {
    val dependencies = module {
        single { MongoContext(config.connectionStrings.mongodb) }
        single { KafkaConsumer<UUID, Task>(consumerProperties) }
    }

    modules(dependencies)
}