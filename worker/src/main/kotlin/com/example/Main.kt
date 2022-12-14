package com.example

import com.example.data.MongoContext
import com.example.model.TaskCreatedEvent
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import com.sksamuel.hoplite.ConfigLoader
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.dsl.onClose
import sun.misc.Signal
import java.util.*

private val logger = KotlinLogging.logger {}
fun main(args: Array<String>) = runBlocking {
    Signal.handle(Signal("INT")) {
        this.coroutineContext.cancelChildren()
    }

    logger.info { "Starting worker" }
    
    val config = ConfigLoader().loadConfigOrThrow<Config>("/config.yaml")
    val consumerProperties = javaClass.classLoader.getResourceAsStream("consumer.properties").use {
        Properties().apply {
            load(it)
        }
    }

    val koinApplication = startKoin {
        installDependencies(config, consumerProperties)
    }

    try {
        startApp(koinApplication)
    }
    catch (_: CancellationException){}
    catch (exception: Throwable) {
        logger.error(exception) { "Worker exited unexpectedly" }
    }
    finally {
        stopKoin()
    }
}

suspend fun startApp(koinApplication: KoinApplication) = coroutineScope<Unit> {
    launch {
        koinApplication.koin.get<Consumer>().run()
    }

    launch {
        koinApplication.koin.get<Watcher>().run()
    }
}

fun KoinApplication.installDependencies(config: Config, consumerProperties: Properties) {
    val dockerClientConfig = DefaultDockerClientConfig.Builder()
        .withDockerHost("unix:///var/run/docker.sock")
        .build()

    val dockerHttpClient = ZerodepDockerHttpClient.Builder()
        .dockerHost(dockerClientConfig.dockerHost)
        .build()

    val dockerClient = DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient)

    val dependencies = module {
        single { MongoContext(config.connectionStrings.mongodb) }
        single { KafkaConsumer<UUID, TaskCreatedEvent>(consumerProperties) } onClose { it?.close() }
        single { dockerClient } onClose { it?.close() }
        singleOf(::Launcher)
        singleOf(::Watcher)
        singleOf(::Consumer)
    }

    modules(dependencies)
}