package com.example

import com.example.data.MongoContext
import com.example.model.Task
import com.sksamuel.hoplite.ConfigLoader
import kotlinx.coroutines.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.dsl.onClose
import sun.misc.Signal
import java.util.*

fun main(args: Array<String>) = runBlocking<Unit> {
    Signal.handle(Signal("INT")) {
        this.coroutineContext.cancelChildren()
        stopKoin()
    }

    val config = ConfigLoader().loadConfigOrThrow<Config>("./config.yaml")
    val producerProperties = javaClass.classLoader.getResourceAsStream("producer.properties").use {
        Properties().apply {
            load(it)
        }
    }

    startKoin {
        installDependencies(config, producerProperties)
    }

    launch {
        DispatcherJob().execute()
    }
}

fun KoinApplication.installDependencies(config: Config, producerProperties: Properties) {
    val dependencies = module {
        single { MongoContext(config.connectionStrings.mongodb) }
        single { KafkaProducer<String, Task>(producerProperties) } onClose {
            it?.close()
        }
    }

    modules(dependencies)
}