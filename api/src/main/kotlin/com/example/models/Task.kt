package com.example.models

import com.example.utils.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

enum class TargetFrameworkMonikier(val value: String) {
    Net60("net6.0")
}

@Serializable
data class NugetPackage(
    val name: String,
    val version: String
)

enum class TaskStatus {
    Pending,
    Executing,
    Finished
}

@Serializable
class Task (
    @Serializable(with = UUIDSerializer::class)
    @SerialName("_id")
    val id: UUID,
    val code: String,
    val arguments: List<String>? = null,
    val targetFrameworkMonikier: TargetFrameworkMonikier,
    val status: TaskStatus,
    val nugetPackages: List<NugetPackage>? = null
)