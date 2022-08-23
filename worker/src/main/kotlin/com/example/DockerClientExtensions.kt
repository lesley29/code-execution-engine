package com.example

import com.github.dockerjava.api.command.SyncDockerCmd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> SyncDockerCmd<T>.execute() = withContext<T>(Dispatchers.IO) {
    this@execute.exec()
}