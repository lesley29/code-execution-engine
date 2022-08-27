package com.example

import com.example.data.MongoContext
import com.example.model.Task
import com.example.model.TaskStatus
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.BuildImageCmd
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.exception.ConflictException
import com.github.dockerjava.api.model.Capability
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.LogConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.litote.kmongo.*
import utils.execute
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Launcher(
    private val client: DockerClient,
    private val mongoContext: MongoContext
) {
    private val imageTemplateBytes: ByteArray = javaClass.classLoader.getResourceAsStream("template.Dockerfile").use {
        it?.readAllBytes() ?: byteArrayOf()
    }

    suspend fun startTask(task: Task) {
        val updatedTask = mongoContext.tasks.findOneAndUpdate(
            and(
                Task::id eq task.id,
                Task::status `in` listOf(TaskStatus.Executing, TaskStatus.Pending, TaskStatus.Created)
            ),
            set(Task::status setTo TaskStatus.Executing)
        )

        updatedTask ?: return

        client.buildImageCmd(prepareTar(task)).use {
            it
                .withBuildArg("TARGET_FRAMEWORK", "6.0")
                .withTags(setOf(task.id.toString()))
                .execute()
        }

        val createNetworkResponse = client.createNetworkCmd()
            .withName(task.id.toString())
            .execute()

        val memoryLimitBytes = 64 * 1024 * 1024L
        try {
            val createContainerResponse = client.createContainerCmd(task.id.toString())
                .withHostConfig(
                    HostConfig.newHostConfig()
                        .withCapDrop(Capability.ALL)
                        .withMemory(memoryLimitBytes)
                        .withCpuPeriod(100000)
                        .withCpuQuota(10000)
                        .withNetworkMode(createNetworkResponse.id)
                        .withLogConfig(LogConfig(LogConfig.LoggingType.JSON_FILE))
                )
                .withName(task.id.toString())
                .withCmd(task.arguments ?: listOf())
                .execute()

            client.startContainerCmd(createContainerResponse.id).execute()
        } catch (conflict: ConflictException) {
            println("Container with the same name already exists, do nothing")
        }
    }

    private fun prepareTar(task: Task): InputStream {
        val outputByteStream = ByteArrayOutputStream()

        TarArchiveOutputStream(BufferedOutputStream(outputByteStream)).use { tarStream ->
            val packages = task.nugetPackages
                ?.joinToString("\n") { "${it.name} ${it.version}" }
                ?.toByteArray()

            tarStream.addEntry("packages.txt", packages ?: byteArrayOf())
            tarStream.addEntry("Program.cs", task.code.toByteArray())
            tarStream.addEntry("Dockerfile", imageTemplateBytes)

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

private suspend fun BuildImageCmd.execute() = suspendCancellableCoroutine { cont ->
    val callback = object: BuildImageResultCallback(){
        override fun onComplete() {
            super.onComplete()
            cont.resume(Unit)
        }

        override fun onError(throwable: Throwable?) {
            super.onError(throwable)
            cont.resumeWithException(throwable!!)
        }
    }
    cont.invokeOnCancellation { callback.close() }
    exec(callback)
}
