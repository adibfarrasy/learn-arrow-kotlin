package learn.arrow.kotlin.app.tutorials

import arrow.continuations.Effect
import org.junit.jupiter.api.Test

// reference: https://arrow-kt.io/docs/patterns/monads/

data class Just<out A>(val value: A)

fun interface JustEffect<A> : Effect<Just<A>> {
    suspend fun <B> Just<B>.bind(): B = value
}

object effect {
    operator fun <A> invoke(func: suspend JustEffect<*>.() -> A): Just<A> =
        Effect.restricted(eff = { JustEffect { it } }, f = func, just = { Just(it) })
}

class MonadLawTest {
    @Test
    fun testLeftIdentity() {
        fun f(x: Int): Just<Int> = Just(x)
        val x = 1

        val a = effect {
            val x2 = Just(x).bind()
            f(x2).bind()
        }

        val b = effect {
            f(x).bind()
        }

        assert(a == b)
    }

    @Test
    fun testRightIdentity() {
        val m = Just(0)

        val a = effect {
            val x = m.bind()
            Just(x).bind()
        }

        val b = effect {
            m.bind()
        }

        assert(a == b)
    }

    @Test
    fun testAssociativity() {
        val m = Just(0)
        fun f(x: Int): Just<Int> = Just(x)
        fun g(x: Int): Just<Int> = Just(x + 1)

        val a = effect {
            val y = effect {
                val x = m.bind()
                f(x).bind()
            }.bind()
            g(y).bind()
        }

        val b = effect {
            val x = m.bind()
            effect {
                val y = f(x).bind()
                g(y).bind()
            }.bind()
        }

        val c = effect {
            val x = m.bind()
            val y = f(x).bind()
            g(y).bind()
        }

        assert(a == b && a == c && b == c)
    }
}