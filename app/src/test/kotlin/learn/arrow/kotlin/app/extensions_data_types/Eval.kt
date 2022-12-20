package learn.arrow.kotlin.app.extensions_data_types

import arrow.core.Eval
import org.junit.jupiter.api.Test

class EvalT {
    private fun even(n: Int): Eval<Boolean> =
        Eval.always { n == 0 }.flatMap {
            if (it) Eval.now(true)
            else odd(n - 1)
        }

    private fun odd(n: Int): Eval<Boolean> =
        Eval.always { n == 0 }.flatMap {
            if (it) Eval.now(false)
            else even(n - 1)
        }

    @Test
    fun testRecursiveEval() {
        assert(!odd(100000).value()) // if not evaled, it will cause StackOverflowError
    }
}