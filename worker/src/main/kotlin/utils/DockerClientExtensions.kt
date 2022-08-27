package utils

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.AsyncDockerCmd
import com.github.dockerjava.api.command.SyncDockerCmd
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

suspend fun <T> SyncDockerCmd<T>.execute() = withContext<T>(Dispatchers.IO) {
    this@execute.exec()
}

fun <TCommand : AsyncDockerCmd<TCommand, TResult>?, TResult> AsyncDockerCmd<TCommand, TResult>.asFlow() = callbackFlow {
    val callback = object : ResultCallback.Adapter<TResult>() {
        override fun onNext(`object`: TResult) {
            `object` ?: return
            trySendBlocking(`object`)
        }

        override fun onComplete() {
            super.onComplete()
            channel.close()
        }

        override fun onError(throwable: Throwable?) {
            super.onError(throwable)
            cancel(CancellationException("Docker API error", throwable))
        }
    }

    exec(callback)
    awaitClose { callback.close() }
}