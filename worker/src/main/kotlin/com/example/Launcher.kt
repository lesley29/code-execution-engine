package com.example

import com.example.model.Task
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Capability
import com.github.dockerjava.api.model.HostConfig
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class Launcher(private val client: DockerClient) {
    private val imageTemplateBytes: ByteArray = Files
        .readAllBytes(Path.of("")) // TODO

    suspend fun start(task: Task) {
        val imageName = "task:${task.id}"
        client.buildImageCmd(prepareTar(task)).use {
            it
                .withBuildArg("TARGET_FRAMEWORK", "6.0")
                .withTags(setOf(imageName))
                .start()
                .awaitImageId()
        }

        val createNetworkResponse = client.createNetworkCmd()
            .withName("task:${task.id}")
            .exec()

        val memoryLimitBytes = 64 * 1024 * 1024L
        val response = client.createContainerCmd(imageName)
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withCapDrop(Capability.ALL)
                    .withMemory(memoryLimitBytes)
                    .withCpuPeriod(100000)
                    .withCpuQuota(10000)
                    .withNetworkMode(createNetworkResponse.id)
            )
            .withCmd(task.arguments ?: listOf())
            .exec()

        client.startContainerCmd(response.id).exec()
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
