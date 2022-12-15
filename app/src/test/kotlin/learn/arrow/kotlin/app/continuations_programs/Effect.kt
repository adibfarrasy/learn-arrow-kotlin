package learn.arrow.kotlin.app.continuations_programs

import arrow.core.Either
import arrow.core.continuations.Effect
import arrow.core.continuations.effect
import arrow.core.continuations.ensureNotNull
import arrow.core.identity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException

object EmptyPath

fun readFile(path: String): Effect<EmptyPath, Unit> = effect {
    if (path.isEmpty()) shift(EmptyPath) else Unit
}

fun readFile2(path: String?): Effect<EmptyPath, Unit> = effect {
    ensureNotNull(path) { EmptyPath }
    ensure(path.isNotEmpty()) { EmptyPath }
}

@JvmInline
value class Content(val body: List<String>)
sealed interface FileError

@JvmInline
value class SecurityError(val msg: String?) : FileError

@JvmInline
value class FileNotFound(val path: String) : FileError
object EmptyPath2 : FileError {
    override fun toString() = "EmptyPath"
}

fun readFile3(path: String?): Effect<FileError, Content> = effect {
    ensureNotNull(path) { EmptyPath2 }
    ensure(path.isNotEmpty()) { EmptyPath2 }
    try {
        val lines = File(path).readLines()
        Content(lines)
    } catch (e: FileNotFoundException) {
        shift(FileNotFound(path))
    } catch (e: SecurityException) {
        shift(SecurityError(e.message))
    }
}

class Effect {
    @Test
    fun testReadEmptyFile() {
        runBlocking {
            val readEmptyPath = readFile("").toEither()

            assert(readEmptyPath.isLeft())
            assert(readEmptyPath == Either.Left(EmptyPath))
        }
    }

    @Test
    fun testReadFile() {
        runBlocking {
            val readFile = readFile("I'm not empty").toEither()

            assert(readFile.isRight())
            assert(readFile == Either.Right(Unit))
        }
    }

    @Test
    fun testReadEmptyFile2() {
        runBlocking {
            val readEmptyPath = readFile2("").toEither()

            assert(readEmptyPath.isLeft())
            assert(readEmptyPath == Either.Left(EmptyPath))
        }
    }

    @Test
    fun testReadFile2() {
        runBlocking {
            val readFile = readFile2("I'm not empty").toEither()

            assert(readFile.isRight())
            assert(readFile == Either.Right(Unit))
        }
    }

    @Test
    fun testReadEmptyFile3() {
        runBlocking {
            val readFile = readFile3("").toEither()

            assert(readFile.isLeft())
            assert(readFile == Either.Left(EmptyPath2))
        }
    }

    @Test
    fun testReadFile3() {
        runBlocking {
            val readFile = readFile3("build.gradle.kts").toEither()

            assert(readFile.isRight())
            println(readFile)
        }
    }
}

class HandlingErrors {
    private val failed: Effect<String, Int> = effect { shift("failed") }
    private val resolved: Effect<Nothing, Int> = failed.handleError { it.length }
    private val newError: Effect<List<Char>, Int> = failed.handleErrorWith { str ->
        effect { shift(str.reversed().toList()) }
    }
    private val redeemed: Effect<Nothing, Int> = failed.redeem({ str -> str.length }, ::identity)
    private val captured: Effect<String, Result<Int>> = effect<String, Int> { 1 }.attempt()

    @Test
    fun testErrorHandling() {
        runBlocking {
            assert(failed.toEither() == Either.Left("failed"))
            assert(resolved.toEither() == Either.Right(6))
            assert(newError.toEither() == Either.Left(listOf('d', 'e', 'l', 'i', 'a', 'f')))
            assert(redeemed.toEither() == Either.Right(6))
            assert(captured.toEither() == Either.Right(Result.success(1)))
        }
    }
}