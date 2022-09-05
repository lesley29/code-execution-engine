package com.example.model

import com.example.utils.InstantSerializer
import com.example.utils.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

object TargetFrameworkMonikier {
    const val net60 = "net6.0"

    val all = listOf(net60)
}

@Serializable
data class NugetPackage(
    val name: String,
    val version: String
)

enum class TaskStatus {
    Created,
    Pending,
    Failed,
    Executing,
    Finished
}

@Serializable
class Task(
    @Serializable(with = UUIDSerializer::class)
    @SerialName("_id")
    val id: UUID,
    val code: String,
    val arguments: List<String>? = null,
    val targetFrameworkMonikier: String,
    val status: TaskStatus,
    val nugetPackages: List<NugetPackage>? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val exitCode: Int? = null,
    val stdout: List<String> = listOf(),
    val stderr: List<String> = listOf(),
    val imageBuildError: ImageBuildError? = null,
    val taskExecutionCompleted: Boolean = false,
    val logStreamingCompleted: Boolean = false,
)

@Serializable
class ImageBuildError(
    val errorDetails: String,
    val restoreError: String? = null,
    val buildError: String? = null
)

@Serializable
data class TaskCreatedEvent(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val code: String,
    val arguments: List<String>? = null,
    val targetFrameworkMonikier: String,
    val nugetPackages: List<NugetPackage>? = null,
)