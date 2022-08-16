package com.example

import com.example.data.MongoContext
import com.example.model.Task
import com.sksamuel.hoplite.ConfigLoader
import kotlinx.coroutines.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.dsl.onClose
import sun.misc.Signal
import java.util.*

fun main(args: Array<String>) = runBlocking<Unit> {
    Signal.handle(Signal("INT")) {
        this.coroutineContext.cancelChildren()
    }

    val config = ConfigLoader().loadConfigOrThrow<Config>("./config.yaml")
    val producerProperties = javaClass.classLoader.getResourceAsStream("producer.properties").use {
        Properties().apply {
            load(it)
        }
    }

    val koinApplication = startKoin {
        installDependencies(config, producerProperties)
    }

    try {
        startApp(koinApplication)
    }
    catch (_: CancellationException){}
    finally {
        stopKoin()
    }
}

suspend fun startApp(koinApplication: KoinApplication) = coroutineScope {
    launch {
        koinApplication.koin.get<TaskDispatcher>().run()
    }
}

fun KoinApplication.installDependencies(config: Config, producerProperties: Properties) {
    val dependencies = module {
        single { MongoContext(config.connectionStrings.mongodb) }
        single { KafkaProducer<UUID, Task>(producerProperties) } onClose { it?.close() }
        singleOf(::TaskDispatcher)
    }

    modules(dependencies)
}