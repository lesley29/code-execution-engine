package com.example

import com.example.data.MongoContext
import com.example.model.*
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.ConflictException
import com.github.dockerjava.api.model.Capability
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.LogConfig
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.litote.kmongo.*
import utils.ImageBuildException
import utils.execute
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class Launcher(
    private val client: DockerClient,
    private val mongoContext: MongoContext
) {
    private val imageTemplateBytes: ByteArray = javaClass.classLoader.getResourceAsStream("template.Dockerfile").use {
        it?.readAllBytes() ?: byteArrayOf()
    }

    private val entrypointBytes: ByteArray = javaClass.classLoader.getResourceAsStream("entrypoint.sh").use {
        it?.readAllBytes() ?: byteArrayOf()
    }

    private val tfmToTag = mapOf(
        TargetFrameworkMonikier.net60 to "6.0"
    )

    suspend fun startTask(taskCreatedEvent: TaskCreatedEvent) {
        val updatedTask = mongoContext.tasks.findOneAndUpdate(
            and(
                Task::id eq taskCreatedEvent.id,
                Task::status `in` listOf(TaskStatus.Executing, TaskStatus.Pending, TaskStatus.Created)
            ),
            set(Task::status setTo TaskStatus.Executing)
        )

        updatedTask ?: return

        val buildImageCommand = client.buildImageCmd(prepareTar(taskCreatedEvent))
        try {
            buildImageCommand
                .withBuildArg("TARGET_FRAMEWORK", tfmToTag[taskCreatedEvent.targetFrameworkMonikier])
                .withTags(setOf(taskCreatedEvent.id.toString()))
                .execute()
        } catch (exception: ImageBuildException) {
            mongoContext.tasks.findOneAndUpdate(
                Task::id eq taskCreatedEvent.id,
                set(
                    Task::status setTo TaskStatus.Failed,
                    Task::imageBuildError setTo ImageBuildError(
                        exception.clientError,
                        exception.restoreError,
                        exception.buildError
                    )
                )
            )
            return
        } finally {
            buildImageCommand.close()
        }

        val createNetworkResponse = client.createNetworkCmd()
            .withName(taskCreatedEvent.id.toString())
            .execute()

        val memoryLimitBytes = 64 * 1024 * 1024L
        try {
            val createContainerResponse = client.createContainerCmd(taskCreatedEvent.id.toString())
                .withHostConfig(
                    HostConfig.newHostConfig()
                        .withCapDrop(Capability.ALL)
                        .withMemory(memoryLimitBytes)
                        .withCpuPeriod(100000)
                        .withCpuQuota(10000)
                        .withNetworkMode(createNetworkResponse.id)
                        .withLogConfig(LogConfig(LogConfig.LoggingType.JSON_FILE))
                )
                .withName(taskCreatedEvent.id.toString())
                .withCmd(taskCreatedEvent.arguments ?: listOf())
                .execute()

            client.startContainerCmd(createContainerResponse.id).execute()
        } catch (conflict: ConflictException) {
            println("Container with the same name already exists, do nothing")
        }
    }

    private fun prepareTar(task: TaskCreatedEvent): InputStream {
        val outputByteStream = ByteArrayOutputStream()

        TarArchiveOutputStream(BufferedOutputStream(outputByteStream)).use { tarStream ->
            val packages = task.nugetPackages
                ?.joinToString("\n") { "${it.name} ${it.version}" }
                ?.toByteArray()

            tarStream.addEntry("packages.txt", packages ?: byteArrayOf())
            tarStream.addEntry("Program.cs", task.code.toByteArray())
            tarStream.addEntry("Dockerfile", imageTemplateBytes)
            tarStream.addEntry("entrypoint.sh", entrypointBytes)

            tarStream.finish()
        }

        return ByteArrayInputStream(outputByteStream.toByteArray())
    }
}

private fun TarArchiveOutputStream.addEntry(name: String, data: ByteArray) {
    val entry = TarArchiveEntry(name)
    entry.size = data.size.toLong()
    putArchiveEntry(entry)
    write(data)
    closeArchiveEntry()
}
