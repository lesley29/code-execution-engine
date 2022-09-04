package utils

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.AsyncDockerCmd
import com.github.dockerjava.api.command.BuildImageCmd
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.command.SyncDockerCmd
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
        private var restoreError: String? = null
        private var buildError: String? = null
        private var clientError: ResponseItem.ErrorDetail? = null

        override fun onNext(item: BuildResponseItem?) {
            item ?: return
            super.onNext(item)

            val isRestoreError = item.stream?.contains("error: NU") == true
            if (isRestoreError) {
                restoreError = item.stream
            }

            val isBuildError = item.stream?.contains("error CS") == true
            if (isBuildError) {
                buildError = item.stream
            }

            if (item.isErrorIndicated) {
                clientError = item.errorDetail
            }

            if (item.isBuildSuccessIndicated) {
                imageId = item.imageId
            }
        }

        override fun onComplete() {
            super.onComplete()
            if (imageId != null) {
                cont.resume(imageId)
            } else {
                cont.resumeWithException(ImageBuildException(
                    clientError!!.toString(),
                    restoreError,
                    buildError
                ))
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

class ImageBuildException(
    val clientError: String,
    val restoreError: String?,
    val buildError: String?
) : Exception(clientError)