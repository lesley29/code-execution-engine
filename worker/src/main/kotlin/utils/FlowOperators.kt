package utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.chunked(interval: Duration): Flow<List<T>> {
    val intervalMs = interval.toMillis()
    val buffer = Channel<T>(Channel.UNLIMITED)
    return channelFlow {
        coroutineScope {
            launch {
                this@chunked.collect {
                    buffer.send(it)
                }
                buffer.close()
            }

            launch {
                while (!buffer.isClosedForReceive) {
                    val result = mutableListOf<T>()
                    while (!buffer.isClosedForReceive && !buffer.isEmpty) {
                        buffer.tryReceive()
                            .onSuccess { result.add(it) }
                    }

                    if (result.any()) {
                        this@channelFlow.send(result)
                    }
                    delay(intervalMs)
                }
            }
        }
    }
}