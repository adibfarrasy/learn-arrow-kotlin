package learn.arrow.kotlin.app.extensions_data_types

import arrow.core.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class EitherT {
    @Test
    fun testRightBiasedOperations() {
        val right: Either<String, Int> = Either.Right(5)
        val value = right.flatMap { Either.Right(it + 1) }

        assert(value == Either.Right(6))

        val left: Either<String, Int> = Either.Left("Something went wrong")
        val value2 = left.flatMap { Either.Right(it + 1) }

        assert(value2 == Either.Left("Something went wrong"))
    }

    sealed class Error {
        object NotANumber : Error()
        object NoZeroReciprocal : Error()
        object GenericError : Error()
    }

    private fun parse(s: String): Either<Error, Int> =
        if (s.matches("-?[0-9]+".toRegex())) Either.Right(s.toInt())
        else Either.Left(Error.NotANumber)

    private fun reciprocal(i: Int): Either<Error, Double> =
        if (i == 0) Either.Left(Error.NoZeroReciprocal)
        else Either.Right(1.0 / i)

    private fun stringify(d: Double): String = d.toString()

    private fun magic(s: String): Either<Error, String> =
        parse(s).flatMap { reciprocal(it) }.map { stringify(it) }

    @Test
    fun testCaptureExceptions() {
        val parsed1 = parse("Not a number")
        val parsed2 = parse("2")

        assert(parsed1.isLeft())
        assert(parsed2.getOrHandle { "Shouldn't go here" } == 2)

        val magic1 = magic("0")
        val magic2 = magic("1")
        val magic3 = magic("NaN")

        assert(magic1.isLeft())
        assert(magic2.getOrHandle { "Shouldn't go here" } == "1.0")
        assert(magic3.isLeft())

        val value = when (val x = magic("2")) {
            is Either.Left -> when (x.value) {
                is Error.NotANumber -> "Not a number!"
                is Error.NoZeroReciprocal -> "Can't take reciprocal of 0!"
                is Error.GenericError -> "Can't go here"
            }

            is Either.Right -> "Got reciprocal: ${x.value}"
        }

        assert(value == "Got reciprocal: 0.5")
    }

    @Test
    fun testEitherCatchingException() {
        fun throwException(): String = throw RuntimeException("blow up!")
        fun noSideEffects(): Either<Error, String> = Either.catch { throwException() }.mapLeft { Error.GenericError }

        assert(noSideEffects() == Either.Left(Error.GenericError))
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun <A> handleSuccess(log: suspend (a: A) -> Either<Throwable, Unit>, a: A): Either<Throwable, Response> =
        Either.catch {
            Response.Builder(HttpStatus.OK)
                .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                .body(a)
                .build()
        }

    @Suppress("UNUSED_PARAMETER")
    suspend fun <E> handleError(log: suspend (e: E) -> Either<Throwable, Unit>, e: E): Either<Throwable, Response> =
        createErrorResponse(HttpStatus.NOT_FOUND, ErrorResponse("$ERROR_MESSAGE_PREFIX $e"))

    suspend fun handleThrowable(
        log: suspend (throwable: Throwable) -> Either<Throwable, Unit>,
        throwable: Throwable
    ): Either<Throwable, Response> =
        log(throwable)
            .flatMap {
                createErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorResponse("$THROWABLE_MESSAGE_PREFIX $throwable")
                )
            }

    suspend fun createErrorResponse(httpStatus: HttpStatus, errorResponse: ErrorResponse): Either<Throwable, Response> =
        Either.catch {
            Response.Builder(httpStatus)
                .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                .body(errorResponse)
                .build()
        }

    suspend fun log(level: Level, message: String): Either<Throwable, Unit> =
        Unit.right() // Should implement logging.

    enum class HttpStatus(val value: Int) { OK(200), NOT_FOUND(404), INTERNAL_SERVER_ERROR(500) }

    class Response private constructor(
        val status: HttpStatus,
        val headers: Map<String, String>,
        val body: Any?
    ) {

        data class Builder(
            val status: HttpStatus,
            var headers: Map<String, String> = emptyMap(),
            var body: Any? = null
        ) {
            fun header(key: String, value: String) =
                apply { this.headers = this.headers + mapOf<String, String>(key to value) }

            fun body(body: Any?) = apply { this.body = body }
            fun build() = Response(status, headers, body)
        }
    }

    val CONTENT_TYPE = "Content-Type"
    val CONTENT_TYPE_APPLICATION_JSON = "application/json"
    val ERROR_MESSAGE_PREFIX = "An error has occurred. The error is:"
    val THROWABLE_MESSAGE_PREFIX = "An exception was thrown. The exception is:"

    sealed class Errorz {
        object SpecificError : Errorz()
    }

    data class ErrorResponse(val errorMessage: String)
    enum class Level { INFO, WARN, ERROR }

    @Test
    fun testEitherResolve() {
        suspend fun httpEndpoint(request: String = "Hello?") =
            Either.resolve(
                f = {
                    if (request == "Hello?") "HELLO WORLD!".right()
                    else Errorz.SpecificError.left()
                },
                success = { a -> handleSuccess({ a: Any -> log(Level.INFO, "This is a: $a") }, a) },
                error = { e -> handleError({ e: Any -> log(Level.WARN, "This is e: $e") }, e) },
                throwable = { throwable ->
                    handleThrowable(
                        { throwable: Throwable -> log(Level.ERROR, "Log the throwable: $throwable.") },
                        throwable
                    )
                },
                unrecoverableState = { _ -> Unit.right() },
            )

        runBlocking {
            assert(httpEndpoint().status == HttpStatus.OK)
        }
    }

    @Test
    fun testEitherOperations() {
        val right: Either<Int, Int> = Either.Right(7)
        val left: Either<Int, Int> = Either.Left(7)

        val rightMapLeft = right.mapLeft { it + 1 }
        val leftMapLeft = left.mapLeft { it + 1 }

        assert(rightMapLeft.getOrHandle { "Shouldn't go here" } == 7)
        assert(leftMapLeft.getOrHandle { it } == 8)


        assert(right.contains(7))


        val swapped = right.swap()

        assert(swapped.isLeft())

        val someFlag = true
        val conditional = Either.conditionally(someFlag, { "Error" }, { 69 })

        assert(conditional == Either.Right(69))

        val folded = right.fold({ -1 }, { it })
        assert(folded == 7)
    }

    @Test
    fun testEitherHandleNullable() {
        val value1 = Either.Right(42).leftIfNull { -1 }
        assert(value1 == Either.Right(42))

        val value2 = Either.Right(null).leftIfNull { -1 }
        assert(value2 == Either.Left(-1))

        val value3 = Either.Left(12).leftIfNull { -1 }
        assert(value3 == Either.Left(12))

        val value4 = "value".rightIfNotNull { "left" }
        assert(value4 == Either.Right("value"))

        val value5 = null.rightIfNotNull { "left" }
        assert(value5 == Either.Left("left"))

        val value6 = null.rightIfNull { "left" }
        assert(value6 == Either.Right(null))

        val value7 = "value".rightIfNull { "left" }
        assert(value7 == Either.Left("left"))
    }
}