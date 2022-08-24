package com.example

import com.example.model.Task
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.BuildImageCmd
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.model.Capability
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.LogConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import utils.execute
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Launcher(private val client: DockerClient) {
    private val imageTemplateBytes: ByteArray = javaClass.classLoader.getResourceAsStream("template.Dockerfile").use {
        it?.readAllBytes() ?: byteArrayOf()
    }

    suspend fun startTask(task: Task) {
        val imageName = "task:${task.id}"
        client.buildImageCmd(prepareTar(task)).use {
            it
                .withBuildArg("TARGET_FRAMEWORK", "6.0")
                .withTags(setOf(imageName))
                .execute()
        }

        val createNetworkResponse = client.createNetworkCmd()
            .withName("task:${task.id}")
            .execute()

        val memoryLimitBytes = 64 * 1024 * 1024L
        val response = client.createContainerCmd(imageName)
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

        client.startContainerCmd(response.id).execute()
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
