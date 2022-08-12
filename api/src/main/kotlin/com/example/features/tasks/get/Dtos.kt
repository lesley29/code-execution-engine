package com.example.features.tasks.get

import com.example.models.NugetPackage
import com.example.models.TargetFrameworkMonikier
import com.example.models.TaskStatus
import com.example.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class TaskDto (
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val code: String,
    val arguments: List<String>? = null,
    val targetFrameworkMonikier: TargetFrameworkMonikier,
    val status: TaskStatus,
    val nugetPackages: List<NugetPackage>? = null,
)