package melomancool.chip8.util

import kotlin.random.Random
import kotlin.text.MatchResult.Destructured

import org.pcollections.PVector
import org.pcollections.TreePVector

fun <E> filledPVector(size: Int, value: E): PVector<E> {
    return TreePVector.from( List<E>(size) { value } )
}

fun <E> replaceAt(vec: PVector<E>, replacement: PVector<E>, vecAt: Int): PVector<E> {
    val vecBefore = vec.subList(0, vecAt)
    val vecAfter = vec.subList(vecAt + replacement.size, vec.size)
    return vecBefore
        .plusAll( replacement )
        .plusAll( vecAfter )
}

fun pow(x: UInt, exp: Int): UInt {
    tailrec fun powImpl(x: UInt, exp: Int, res: UInt): UInt {
        return if (exp <= 0) { res } else { powImpl(x, exp - 1, res * x) }
    }
    return powImpl(x, exp, 1u)
}

fun shiftLeft(x: UByte, n: Int): UInt {
    // Shifting x left by n bits has the
    // effect of multiplying it by 2^n.
    return x * pow(2u, n)
}

fun toHexString(x: UByte): String {
    return "%02X".format(x.toInt())
}

fun hexOpcodeToRegex(hexOpcode: String): String {
    return hexOpcode
        .replace("nnn", """([0-9A-F][0-9A-F][0-9A-F])""")
        .replace("n", """([0-9A-F])""")
        .replace("x", """([0-9A-F])""")
        .replace("y", """([0-9A-F])""")
        .replace("kk", """([0-9A-F][0-9A-F])""")
}

inline fun <R> matchOpcode(string: String, block: MatchOpcode.() -> R): R =
    MatchOpcode(string).block()

inline class MatchOpcode(val str: String) {
    inline operator fun <R: Any> String.invoke(block: (Destructured) -> R): R? =
        Regex(hexOpcodeToRegex(this)).matchEntire(str)?.destructured?.let(block)
}

fun randomUByte(): UByte = Random.nextInt(0, 255).toUByte()

fun byteToBits(x: UByte): PVector<Boolean> =
    TreePVector.from(x.toString(radix = 2).map { it == '1' })
