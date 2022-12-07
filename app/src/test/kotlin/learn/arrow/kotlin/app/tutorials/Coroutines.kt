package learn.arrow.kotlin.app.tutorials

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume

class Coroutines {
    @Test
    fun testCoroutine() {
        runBlocking {
            suspend fun one() = 1

            val cont: Continuation<Unit> = ::one
                .createCoroutine(Continuation(EmptyCoroutineContext, ::println))

            cont.resume(Unit)
        }
    }

    @Test
    fun testCoroutineResumeMoreThanOnce() {
        runBlocking {
            suspend fun one() = 1

            val cont: Continuation<Unit> = ::one
                .createCoroutine(Continuation(EmptyCoroutineContext, ::println))

            cont.resume(Unit)

            val throws = assertThrows<IllegalStateException>("Should throw when resumed more than once") {
                cont.resume(Unit)
            }

            assertEquals("Already resumed", throws.message)
        }
    }
}