package learn.arrow.kotlin.app.extensions_data_types

import arrow.core.nonEmptyListOf
import org.junit.jupiter.api.Test
import kotlin.random.Random

class NonEmptyListT {
    @Test
    fun testNonEmptyList() {
        val list = nonEmptyListOf(1, 2, 3, 4, 5)
        println(list)

        val head = list.head
        assert(head == 1)

        val sumList = list.foldLeft(0) { acc, n -> acc + n }
        assert(sumList == 15)

        val mappedList = list.map { it + 1 }
        assert(mappedList.head == 2)
    }

    @Test
    fun testCombineNonEmptyList() {
        val nelOne = nonEmptyListOf(1, 2, 3)
        val nelTwo = nonEmptyListOf(4, 5)

        val combined = nelOne.flatMap { one ->
            nelTwo.map { two ->
                one + two
            }
        }

        println(combined)
    }

    data class Person(val id: Long, val name: String, val year: Int)
    @Test
    fun testZip() {
        val nelId = nonEmptyListOf(Random.nextLong(), Random.nextLong())
        val nelName = nonEmptyListOf("William Alvin Howard", "Haskell Curry")
        val nelYear = nonEmptyListOf(1926, 1990)

        val value = nelId.zip(nelName, nelYear) {id, name, year ->
            Person(id, name, year)
        }

        println(value)
    }
}