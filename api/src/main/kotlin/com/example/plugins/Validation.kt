package com.example.plugins

import com.example.features.tasks.create.CreateTaskRequest
import com.example.model.TargetFrameworkMonikier
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<CreateTaskRequest> { request ->
            if (request.code.isEmpty()) {
                return@validate ValidationResult.Invalid("Task must contain some code")
            }

            if (!TargetFrameworkMonikier.all.contains(request.targetFrameworkMonikier)) {
                return@validate ValidationResult.Invalid(
                    "tfm must be one of the following values: [${TargetFrameworkMonikier.all.joinToString()}]")
            }

            request.nugetPackages?.forEach { packageDto ->
                if (packageDto.name.isBlank()) {
                    return@validate ValidationResult.Invalid("Package name must not be empty")
                }

                if (packageDto.version.isBlank()) {
                    return@validate ValidationResult.Invalid("Package version must not be empty")
                }
            }

            ValidationResult.Valid
        }
    }
}