package learn.arrow.kotlin.app.tutorials

import arrow.core.*
import arrow.core.continuations.either
import arrow.core.continuations.nullable
import arrow.typeclasses.Semigroup
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

// reference: https://arrow-kt.io/docs/patterns/error_handling/

object Lettuce
object Knife
object Salad

class ErrorHandlingNullableTest {
    companion object {
        fun takeFoodFromRefrigerator(): Lettuce? = null
        fun getKnife(): Knife? = null
        fun prepare(tool: Knife, ingredient: Lettuce): Salad = Salad
    }

    @Test
    fun testPrepareLunch() {
        fun prepareLunch(): Salad? {
            val lettuce = takeFoodFromRefrigerator()
            val knife = getKnife()
            val salad = knife?.let { k -> lettuce?.let { l -> prepare(k, l) } }
            return salad
        }

        assert(prepareLunch() == null)
    }

    @Test
    fun testPrepareLunchWithArrowNulalble() {
        suspend fun prepareLunch(): Salad? {
            return nullable {
                val lettuce = takeFoodFromRefrigerator().bind()
                val knife = getKnife().bind()
                val salad = knife?.let { k -> lettuce?.let { l -> prepare(k, l) } }
                salad
            }
        }

        runBlocking {
            assert(prepareLunch() == null)
        }
    }
}

sealed class CookingException {
    object NastyLettuce : CookingException()
    object KnifeIsDull : CookingException()
    data class InsufficientAmountOfLettuce(val quantityInGrams: Int) : CookingException()
}

typealias NastyLettuce = CookingException.NastyLettuce
typealias KnifeIsDull = CookingException.KnifeIsDull
typealias InsufficientAmountOfLettuce = CookingException.InsufficientAmountOfLettuce

class ErrorHandlingADTTest {
    companion object {
        fun takeFoodFromRefrigerator(): Either<NastyLettuce, Lettuce> = Either.Right(Lettuce)
        fun getKnife(): Either<KnifeIsDull, Knife> = Either.Right(Knife)
        fun lunch(knife: Knife, food: Lettuce): Either<InsufficientAmountOfLettuce, Salad> = Either.Left(
            InsufficientAmountOfLettuce(5)
        )
    }

    @Test
    fun testPrepareEither() {
        suspend fun prepareEither(): Either<CookingException, Salad> {
            return either {
                val lettuce = takeFoodFromRefrigerator().bind()
                val knife = getKnife().bind()
                val salad = lunch(knife, lettuce).bind()
                salad
            }
        }

        runBlocking {
            assert(prepareEither().isLeft())
        }
    }
}

sealed class ValidationError(val msg: String) {
    data class DoesNotContain(val value: String) : ValidationError("Did not contain $value")
    data class MaxLength(val value: Int) : ValidationError("Exceeded length of $value")
    data class NotAnEmail(val reasons: Nel<ValidationError>) : ValidationError("Not a valid email")
}

data class FormField(val label: String, val value: String)
data class Email(val value: String)

sealed class Strategy {
    object FailFast : Strategy()
    object ErrorAccumulation : Strategy()
}

object Rules {
    private fun FormField.contains(needle: String): ValidatedNel<ValidationError, FormField> {
        return if (value.contains(needle, false)) validNel()
        else ValidationError.DoesNotContain(needle).invalidNel()
    }

    private fun FormField.maxLength(maxLength: Int): ValidatedNel<ValidationError, FormField> {
        return if (value.length <= maxLength) validNel()
        else ValidationError.MaxLength(maxLength).invalidNel()
    }

    private fun FormField.validateErrorAccumulate(): ValidatedNel<ValidationError, Email> {
        return contains("@").zip(
            Semigroup.nonEmptyList(),
            maxLength(250)
        ) { _, _ -> Email(value) }.handleErrorWith { ValidationError.NotAnEmail(it).invalidNel() }
    }

    private fun FormField.validateFailFast(): Either<Nel<ValidationError>, Email> {
        return either.eager {
            contains("@").bind()
            maxLength(250).bind()
            Email(value)
        }
    }

    operator fun invoke(strategy: Strategy, fields: List<FormField>): Either<Nel<ValidationError>, List<Email>> {
        return when (strategy) {
            Strategy.FailFast -> fields.traverse { it.validateFailFast() }
            Strategy.ErrorAccumulation -> fields.traverse { it.validateErrorAccumulate() }.toEither()
        }
    }
}

class ErrorHandlingStrategy {
    private val fields = listOf(
        FormField("Invalid Email Domain Label", "nowhere.com"),
        FormField("Too long Email Label", "nowheretoolong${(0..251).map { "g" }}"),
        FormField("Valid Email Label", "getlost@nowhere.com"),
    )

    @Test
    fun testFailFastRule() {
        assert(Rules(Strategy.FailFast, fields).isLeft())
    }

    @Test
    fun testAccumulateErrorRule() {
        val result = Rules(Strategy.ErrorAccumulation, fields)
        assert(result.isLeft())
        assert(result.getOrHandle { it.size } == 2)
    }
}