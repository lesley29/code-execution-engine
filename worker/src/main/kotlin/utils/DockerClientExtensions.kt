package utils

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.AsyncDockerCmd
import com.github.dockerjava.api.command.BuildImageCmd
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.command.SyncDockerCmd
import com.github.dockerjava.api.exception.DockerClientException
import com.github.dockerjava.api.model.BuildResponseItem
import com.github.dockerjava.api.model.ResponseItem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

suspend fun BuildImageCmd.execute() = suspendCancellableCoroutine { cont ->
    val callback = object: BuildImageResultCallback() {
        private var imageId: String? = null
        private var error: ResponseItem.ErrorDetail? = null

        override fun onNext(item: BuildResponseItem?) {
            item ?: return
            super.onNext(item)

            if (item.isBuildSuccessIndicated) {
                imageId = item.imageId
            }

            if (item.isErrorIndicated) {
                error = item.errorDetail
            }
        }

        override fun onComplete() {
            super.onComplete()
            if (imageId != null) {
                cont.resume(imageId)
            } else {
                cont.resumeWithException(DockerClientException(error.toString()))
            }
        }

        override fun onError(throwable: Throwable?) {
            super.onError(throwable)
            cont.resumeWithException(throwable!!)
        }
    }

    cont.invokeOnCancellation { callback.close() }
    exec(callback)
}