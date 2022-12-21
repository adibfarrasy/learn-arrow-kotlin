package learn.arrow.kotlin.app.extensions_data_types

import arrow.core.*
import org.junit.jupiter.api.Test
import arrow.core.continuations.option
import kotlinx.coroutines.runBlocking

class OptionT {
    @Test
    fun testOption() {
        val someValue = Some("I am wrapped in something")
        val emptyValue = none<String>()

        assert(someValue == Some("I am wrapped in something"))
        assert(emptyValue == None)
    }

    @Test
    fun testOptionReturn() {
        fun maybeItWillReturnSomething(flag: Boolean): Option<String> =
            if (flag) Some("Found value") else None

        val value1 = maybeItWillReturnSomething(true)
            .getOrElse { "no value" }

        val value2 = maybeItWillReturnSomething(false)
            .getOrElse { "no value" }

        assert(value1 == "Found value")
        assert(value2 == "no value")

        assert(maybeItWillReturnSomething(true) !is None)
        assert(maybeItWillReturnSomething(false) is None)
    }

    @Test
    fun testCreateOptionFromNullable() {
        val myString: String? = "Nullable string"
        val option = Option.fromNullable(myString)
        val option2 = myString.toOption()

        assert(option == Some("Nullable string"))
        assert(option == option2)

        val some = 1.some()
        val none = none<String>()
        assert(some == Some(1))
        assert(none == None)

        val foxMap = mapOf(1 to "the", 2 to "quick", 3 to "brown", 4 to "fox")
        val empty1 = foxMap.entries.firstOrNull { it.key == 5 }?.value.let { it?.toCharArray() }.toOption()
        val empty2 = Option.fromNullable(foxMap.entries.firstOrNull { it.key == 5 }?.value.let { it?.toCharArray() })
        assert(empty1 == None)
        assert(empty2 == None)
    }

    @Test
    fun testOptionSwitchCase() {
        val someValue: Option<Double> = Some(20.0)
        val result = when (someValue) {
            is Some -> someValue.value
            is None -> 0.0
        }

        assert(result == 20.0)
    }

    @Test
    fun testOptionOperations() {
        val number: Option<Int> = Some(3)
        val noNumber: Option<Int> = None
        val mappedResult1 = number.map { it * 2 }
        val mappedResult2 = noNumber.map { it * 2 }

        assert(mappedResult1 == Some(6))
        assert(mappedResult2 == None)

        val folded1 = number.fold({ 1 }, { it * 2 })
        val folded2 = noNumber.fold({ 1 }, { it * 2 })
        assert(folded1 == 6)
        assert(folded2 == 1)

        val zipped = Some(1).zip(Some("Hello"), Some(true), ::Triple)
        assert(zipped == Some(Triple(1, "Hello", true)))
    }

    @Test
    fun testStructuredErrorHandling() {
        runBlocking {
            val result = option {
                val a = Some(1).bind()
                val b = Some(1 + a).bind()
                val c = Some(1 + b).bind()

                a + b + c
            }

            assert(result == Some(6))

            val result2 = option {
                val x = none<Int>().bind()
                val y = Some(1 + x).bind()
                val z = Some(1 + y).bind()

                x + y + z
            }

            assert(result2 == None)
        }
    }
}