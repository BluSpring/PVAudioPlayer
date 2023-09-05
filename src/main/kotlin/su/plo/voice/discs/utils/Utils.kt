package su.plo.voice.discs.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import su.plo.voice.discs.DiscsPlugin
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> suspendSync(task: () -> T): T = suspendCancellableCoroutine { cont ->
    DiscsPlugin.server.execute {
        runCatching(task).fold({ cont.resume(it) }, cont::resumeWithException)
    }
}