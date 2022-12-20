package learn.arrow.kotlin.app.extensions_data_types

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.typeclasses.Semigroup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Semigroup {
    // semigroup basically adds an addition-like operation
    @Test
    fun testAssociativity() {
        val result1 = Semigroup.int().run { (1.combine(2)).combine(3) }
        val result2 = Semigroup.int().run { (3.combine(2)).combine(1) }
        assert(result1 == result2)
    }

    @Test
    fun testSemigroupOption() {
        val result = Semigroup.option(Semigroup.int()).run { Option(1).combine(Option(2)) }
        assert(result == Option(3))

        val result2 = Semigroup.option(Semigroup.int()).run { Option(1).combine(None) }
        assertEquals(result2, Some(1))
    }
}