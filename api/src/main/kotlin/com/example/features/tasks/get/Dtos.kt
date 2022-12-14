package com.example.features.tasks.get

import com.example.model.NugetPackage
import com.example.model.TaskStatus
import com.example.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class TaskDto (
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val code: String,
    val arguments: List<String>? = null,
    val targetFrameworkMonikier: String,
    val status: TaskStatus,
    val nugetPackages: List<NugetPackage>? = null,
    val exitCode: Int? = null,
    val stdout: List<String> = listOf(),
    val stderr: List<String> = listOf(),
    val error: String? = null
)