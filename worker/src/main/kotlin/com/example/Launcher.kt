package com.example

import com.example.model.Task
import com.github.dockerjava.api.DockerClient
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

        val result = client.buildImageCmd(prepareTar(task)).use {
            it
                .withBuildArg("TARGET_FRAMEWORK", "6.0")
                .withTags(setOf("task:${task.id}"))
                .start()
                .awaitImageId()
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
