package com.example.features.tasks.create

import com.example.model.TaskStatus
import com.example.utils.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class NugetPackageDto(
    val name: String,
    val version: String
)

@Serializable
data class CreateTaskRequest(
    val code: String,
    val arguments: List<String>? = null,
    @SerialName("target_framework_monikier")
    val targetFrameworkMonikier: String,
    @SerialName("nuget_packages")
    val nugetPackages: List<NugetPackageDto>? = null
)

@Serializable
data class CreateTaskResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: TaskStatus
)