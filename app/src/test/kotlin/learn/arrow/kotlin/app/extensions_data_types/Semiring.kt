package learn.arrow.kotlin.app.extensions_data_types

import arrow.typeclasses.Semiring
import org.junit.jupiter.api.Test

class Semiring {
    companion object {
        @JvmInline
        value class Money(val value: Int)
        object MoneySemiring : Semiring<Money> {
            override fun one(): Money {
                return Money(1)
            }

            override fun zero(): Money {
                return Money(0)
            }

            override fun Money.combine(b: Money): Money {
                return Money(this.value + b.value)
            }

            override fun Money.combineMultiplicate(b: Money): Money {
                return Money(this.value * b.value)
            }
        }

        private fun Semiring.Companion.money(): Semiring<Money> = MoneySemiring
    }

    @Test
    fun testSemiring() {
        val test1 = Semiring.int().run { 1.combine(2) }
        val test2 = Semiring.int().run { 2.combineMultiplicate(3) }

        assert(test1 == 3)
        assert(test2 == 6)
    }

    @Test
    fun testSemiringOperator() {
        val test1 = Semiring.int().run { 1 + 2 }
        val test2 = Semiring.int().run { 2 * 3 }

        assert(test1 == 3)
        assert(test2 == 6)
    }

    @Test
    fun testCustomSemiring() {
        val test1 = Semiring.money().run { Money(1) + Money(2) }
        val test2 = Semiring.money().run { Money(2) * Money(3) }

        assert(test1 == Money(3))
        assert(test2 == Money(6))
    }
}