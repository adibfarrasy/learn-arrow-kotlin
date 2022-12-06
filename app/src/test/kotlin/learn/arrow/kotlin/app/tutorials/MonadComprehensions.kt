package learn.arrow.kotlin.app.tutorials

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.continuations.either
import arrow.core.flatMap
import arrow.core.getOrHandle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

object NotFound
data class Name(val value: String)
data class UniversityId(val value: String)
data class University(val name: Name, val deanName: Name)
data class Student(val name: Name, val universityId: UniversityId)
data class Dean(val name: Name)

private val students = mapOf(
    Name("Alice") to Student(Name("Alice"), UniversityId("UCA"))
)

private val universities = mapOf(
    UniversityId("UCA") to University(Name("UCA"), Name("James"))
)

private val deans = mapOf(
    Name("James") to Dean(Name("James"))
)

class MonadComprehensions {
    private fun student(name: Name): Either<NotFound, Student> =
        students[name]?.let(::Right) ?: Left(NotFound)

    private fun university(id: UniversityId): Either<NotFound, University> =
        universities[id]?.let(::Right) ?: Left(NotFound)

    private fun dean(name: Name): Either<NotFound, Dean> =
        deans[name]?.let(::Right) ?: Left(NotFound)

    @Test
    fun testFlatMap() {
        val dean = student(Name("Alice")).flatMap { alice ->
            university(alice.universityId).flatMap { university ->
                dean(university.deanName)
            }
        }

        assert(dean.getOrHandle {} == Dean(Name("James")))
    }

    @Test
    fun testEither() {
        runBlocking {
            val dean = either<NotFound, Dean> {
                val alice = student(Name("Alice")).bind()
                val uca = university(alice.universityId).bind()
                val james = dean(uca.deanName).bind()
                james
            }

            assert(dean.getOrHandle {} == Dean(Name("James")))
        }
    }
}