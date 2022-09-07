package com.example

import com.example.data.MongoContext
import com.example.model.TaskCreatedEvent
import com.sksamuel.hoplite.ConfigLoader
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.dsl.onClose
import sun.misc.Signal
import java.util.*

private val logger = KotlinLogging.logger {}
fun main(args: Array<String>) = runBlocking<Unit> {
    Signal.handle(Signal("INT")) {
        this.coroutineContext.cancelChildren()
    }

    val config = ConfigLoader().loadConfigOrThrow<Config>("/config.yaml")

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
    catch (exception: Throwable) {
        logger.error(exception) { "Dispatcher exited unexpectedly" }
    }
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
        single { KafkaProducer<UUID, TaskCreatedEvent>(producerProperties) } onClose { it?.close() }
        singleOf(::TaskDispatcher)
    }

    modules(dependencies)
}