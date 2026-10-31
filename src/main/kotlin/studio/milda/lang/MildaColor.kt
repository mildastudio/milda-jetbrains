package studio.milda.lang

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

// Pure, platform-free colour support for the gutter swatch (MildaColorProvider): read a
// Milda colour literal, say what it looks like in sRGB, and write one back.
//
// This is a SECOND implementation of two things that already exist in TypeScript - the
// DSL's literal grammar (packages/milda/dsl/parse.ts `parseColorFn`) and the sRGB
// conversion with gamut mapping (packages/core/foundations/color.ts). The duplication is
// forced: the swatch has to be drawn synchronously inside the JVM, so it cannot go through
// the Node language server (which is also absent in Community IDEs).
//
// Instead of hoping the copy stays honest, parity is PINNED: `npm run gen:color-goldens`
// runs the real parser and the real colour core over a set of literals and writes
// src/test/resources/color-goldens.json, which MildaColorTest replays against this file.
// Same mechanism as the highlighter's vocabulary parity (MildaVocab.kt).
object MildaColor {
    /** The colour-space functions the DSL spells - the generated vocabulary, so the swatch
     *  appears beside exactly the literals the reader accepts (aliases included). */
    val FUNCTIONS: Set<String> = MildaVocab.COLOR_FNS

    /** `rgba`/`hsla` are the CSS spellings of functions we already have; the maths below is
     *  written for the canonical name, so every entry point resolves the alias first. The
     *  AUTHOR's spelling is what gets written back (see [format]). */
    private val ALIASES: Map<String, String> = mapOf("rgba" to "rgb", "hsla" to "hsl")

    private fun canonical(fn: String): String = ALIASES[fn] ?: fn

    /** sRGB channels in 0..1 plus alpha - what a screen, and a colour picker, deal in. */
    data class Srgb(val r: Double, val g: Double, val b: Double, val a: Double)

    /**
     * The sRGB appearance of `fn(args)`, or null when the arguments aren't a colour this
     * function can read. Percentages, `deg` hues, both alpha spellings (a `/ a` tail and
     * the DSL's older bare fourth channel) and short hex all behave as the parser does.
     */
    fun parse(fn: String, args: String): Srgb? = parseChannels(canonical(fn), args)?.let { toSrgb(it) }

    /** As [parse], but taking the whole literal text (`oklch(0.62 0.19 256)`). */
    fun parse(literal: String): Srgb? {
        val open = literal.indexOf('(')
        if (open <= 0 || !literal.trimEnd().endsWith(")")) return null
        val fn = literal.substring(0, open).trim()
        if (fn !in FUNCTIONS) return null
        return parse(fn, literal.trimEnd().dropLast(1).substring(open + 1))
    }

    /** A `#rgb` / `#rrggbb` / `#rrggbbaa` string, as used by `raw "#…"` escape values. */
    fun parseHex(text: String): Srgb? = hexChannels(text)?.let { toSrgb(it) }

    /** `#rrggbb`, or `#rrggbbaa` when the colour is translucent. */
    fun toHex(c: Srgb): String =
        if (c.a < 1) "#${hex2(c.r)}${hex2(c.g)}${hex2(c.b)}${hex2(c.a)}"
        else "#${hex2(c.r)}${hex2(c.g)}${hex2(c.b)}"

    /**
     * A colour written back in the function the author used, so picking a colour never
     * rewrites `oklch(…)` into a hex string. Note that a wide-gamut value edited through an
     * sRGB picker comes back inside sRGB - unavoidable, and what every other IDE colour
     * picker does too.
     */
    fun format(spelling: String, c: Srgb): String {
        val alpha = if (c.a < 1) " / ${num(c.a, 4)}" else ""
        // The author's own spelling is what goes back into the file, so `rgba(…)` stays
        // `rgba(…)`; only the maths runs under the canonical name.
        val fn = canonical(spelling)
        val name = spelling
        return when (fn) {
            "hex" -> "$name(${toHex(c).removePrefix("#")})"
            "srgb" -> "$name(${num(c.r, 5)} ${num(c.g, 5)} ${num(c.b, 5)}$alpha)"
            "rgb" -> "$name(${byte(c.r)} ${byte(c.g)} ${byte(c.b)}$alpha)"
            "p3" -> {
                val (r, g, b) = fromLinearSrgb("display-p3", decodeGamma(c.r), decodeGamma(c.g), decodeGamma(c.b))
                "$name(${num(r, 5)} ${num(g, 5)} ${num(b, 5)}$alpha)"
            }
            "oklch" -> {
                val (l, chroma, hue) = fromLinearSrgb("oklch", decodeGamma(c.r), decodeGamma(c.g), decodeGamma(c.b))
                "$name(${num(l, 5)} ${num(chroma, 5)} ${num(hue, 3)}$alpha)"
            }
            "hsl" -> {
                val (h, s, l) = fromLinearSrgb("hsl", decodeGamma(c.r), decodeGamma(c.g), decodeGamma(c.b))
                "$name(${num(h, 3)} ${pct(s, 3)} ${pct(l, 3)}$alpha)"
            }
            else -> toHex(c)
        }
    }

    // ─── the literal grammar (twin of parse.ts) ───────────────────────────────

    private data class Channels(val space: String, val ch: DoubleArray, val alpha: Double?)

    // What `N%` means per space, per channel, per CSS Color 4: the reference the percentage
    // is taken OF. null marks a channel where percentages are meaningless (hue is degrees
    // either way) and the number is used verbatim.
    private val CHANNEL_PCT_REF: Map<String, Array<Double?>> = mapOf(
        "srgb" to arrayOf(1.0, 1.0, 1.0),
        "rgb" to arrayOf(255.0, 255.0, 255.0),
        "p3" to arrayOf(1.0, 1.0, 1.0),
        "oklch" to arrayOf(1.0, 0.4, null), // L is 0..1; CSS fixes 100% chroma at 0.4; H is degrees
        "hsl" to arrayOf(null, 1.0, 1.0), // H is degrees; s/l are 0..1
    )

    private fun parseChannels(fn: String, args: String): Channels? {
        if (fn == "hex") return hexChannels(args.trim())

        val refs = CHANNEL_PCT_REF[fn] ?: return null
        // CSS Color 4 separates alpha with a slash; the DSL also accepts a bare 4th channel.
        // Commas are separators and nothing more, so the legacy `rgb(59, 130, 246)` form reads
        // exactly like the modern one (the parser drops them too).
        val parts = args.replace(',', ' ').split("/", limit = 2)
        val words = parts[0].trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size < 3) return null

        val ch = DoubleArray(3)
        for (i in 0..2) {
            val v = channelValue(words[i], refs[i]) ?: return null
            // A hue (null reference) is degrees and never scaled; rgb() authors 0..255.
            ch[i] = if (refs[i] == null) v else v * (if (fn == "rgb") 1.0 / 255.0 else 1.0)
        }
        val alphaWord = if (parts.size > 1) parts[1].trim() else words.getOrNull(3)
        val alpha = alphaWord?.let { channelValue(it, 1.0) }

        val space = when (fn) {
            "rgb" -> "srgb"
            "p3" -> "display-p3"
            else -> fn
        }
        return Channels(space, ch, alpha)
    }

    private fun channelValue(word: String, pctRef: Double?): Double? {
        val w = if (word.length > 3 && word.takeLast(3).lowercase() == "deg") word.dropLast(3) else word
        if (w.endsWith("%")) {
            val n = w.dropLast(1).toDoubleOrNull() ?: return null
            return if (pctRef == null) n else (n / 100.0) * pctRef
        }
        return w.toDoubleOrNull()
    }

    private fun hexChannels(raw: String): Channels? {
        val h0 = raw.trim().removeSurrounding("\"").trim().removePrefix("#")
        if (h0.isEmpty() || !h0.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        // 3 and 4 digits are the shorthands: each digit doubles (`f0c8` = `ff00cc88`).
        val h = if (h0.length == 3 || h0.length == 4) h0.map { "$it$it" }.joinToString("") else h0
        if (h.length != 6 && h.length != 8) return null
        val byte = { k: Int -> h.substring(k * 2, k * 2 + 2).toInt(16) / 255.0 }
        return Channels("srgb", doubleArrayOf(byte(0), byte(1), byte(2)), if (h.length == 8) byte(3) else null)
    }

    // ─── conversion (twin of core/foundations/color.ts) ────────────────────────

    private fun clamp01(n: Double): Double = if (n < 0) 0.0 else if (n > 1) 1.0 else n
    private fun round6(n: Double): Double = (n * 1e6).roundToLong() / 1e6
    private fun norm360(deg: Double): Double = ((deg % 360) + 360) % 360

    // sRGB and Display-P3 share this transfer curve; only their primaries differ.
    private fun decodeGamma(c: Double): Double {
        val a = abs(c)
        val linear = if (a <= 0.04045) a / 12.92 else ((a + 0.055) / 1.055).pow(2.4)
        return if (c < 0) -linear else linear
    }

    private fun encodeGamma(c: Double): Double {
        val a = abs(c)
        val encoded = if (a <= 0.0031308) 12.92 * a else 1.055 * a.pow(1 / 2.4) - 0.055
        return if (c < 0) -encoded else encoded
    }

    // Björn Ottosson's reference matrices.
    private fun linearRgbToOklab(r: Double, g: Double, b: Double): DoubleArray {
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return doubleArrayOf(
            0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s,
        )
    }

    private fun oklabToLinearRgb(bigL: Double, bigA: Double, bigB: Double): DoubleArray {
        val l0 = bigL + 0.3963377774 * bigA + 0.2158037573 * bigB
        val m0 = bigL - 0.1055613458 * bigA - 0.0638541728 * bigB
        val s0 = bigL - 0.0894841775 * bigA - 1.291485548 * bigB
        val l = l0 * l0 * l0
        val m = m0 * m0 * m0
        val s = s0 * s0 * s0
        return doubleArrayOf(
            4.0767416621 * l - 3.3077115913 * m + 0.230969929 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
        )
    }

    // Pre-multiplied D65 matrices (P3 primaries → XYZ → sRGB primaries).
    private fun p3LinearToSrgbLinear(r: Double, g: Double, b: Double): DoubleArray = doubleArrayOf(
        1.2249401762805566 * r - 0.2249401762805563 * g,
        -0.04205697339879597 * r + 1.0420569733987959 * g,
        -0.019637081008389134 * r - 0.07863145834272432 * g + 1.0982685393511135 * b,
    )

    private fun srgbLinearToP3Linear(r: Double, g: Double, b: Double): DoubleArray = doubleArrayOf(
        0.8224621 * r + 0.1775380 * g,
        0.0331941 * r + 0.9668058 * g,
        0.0170827 * r + 0.0723974 * g + 0.9105199 * b,
    )

    private fun hslToSrgb(h: Double, s: Double, l: Double): DoubleArray {
        val c = (1 - abs(2 * l - 1)) * s
        val hp = norm360(h) / 60
        val x = c * (1 - abs((hp % 2) - 1))
        val (r1, g1, b1) = when {
            hp < 1 -> Triple(c, x, 0.0)
            hp < 2 -> Triple(x, c, 0.0)
            hp < 3 -> Triple(0.0, c, x)
            hp < 4 -> Triple(0.0, x, c)
            hp < 5 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        val m = l - c / 2
        return doubleArrayOf(r1 + m, g1 + m, b1 + m)
    }

    private fun srgbToHsl(r: Double, g: Double, b: Double): DoubleArray {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2
        val d = max - min
        if (d < 1e-9) return doubleArrayOf(0.0, 0.0, l)
        val s = d / (1 - abs(2 * l - 1))
        val h = when (max) {
            r -> 60 * (((g - b) / d) % 6)
            g -> 60 * ((b - r) / d + 2)
            else -> 60 * ((r - g) / d + 4)
        }
        return doubleArrayOf(norm360(h), s, l)
    }

    /** Any space's channels as LINEAR sRGB (unclamped: may be out of gamut). */
    private fun toLinearSrgb(space: String, ch: DoubleArray): DoubleArray {
        val a = ch.getOrElse(0) { 0.0 }
        val b = ch.getOrElse(1) { 0.0 }
        val c = ch.getOrElse(2) { 0.0 }
        return when (space) {
            "srgb" -> doubleArrayOf(decodeGamma(a), decodeGamma(b), decodeGamma(c))
            "display-p3" -> p3LinearToSrgbLinear(decodeGamma(a), decodeGamma(b), decodeGamma(c))
            "hsl" -> hslToSrgb(a, b, c).let { doubleArrayOf(decodeGamma(it[0]), decodeGamma(it[1]), decodeGamma(it[2])) }
            "oklch" -> {
                val hr = Math.toRadians(norm360(c))
                oklabToLinearRgb(a, b * cos(hr), b * sin(hr))
            }
            else -> doubleArrayOf(0.0, 0.0, 0.0)
        }
    }

    /** Linear sRGB → a target space's channels (no gamut mapping). */
    private fun fromLinearSrgb(space: String, lr: Double, lg: Double, lb: Double): Triple<Double, Double, Double> =
        when (space) {
            "display-p3" -> srgbLinearToP3Linear(lr, lg, lb)
                .let { Triple(encodeGamma(it[0]), encodeGamma(it[1]), encodeGamma(it[2])) }
            "hsl" -> srgbToHsl(encodeGamma(lr), encodeGamma(lg), encodeGamma(lb))
                .let { Triple(it[0], it[1], it[2]) }
            "oklch" -> {
                val lab = linearRgbToOklab(lr, lg, lb)
                val chroma = sqrt(lab[1] * lab[1] + lab[2] * lab[2])
                // A near-achromatic colour has no meaningful hue; keep it at 0 so
                // floating-point noise can't pick one and destabilise a round-trip.
                val hue = if (chroma < 1e-6) 0.0 else norm360(Math.toDegrees(atan2(lab[2], lab[1])))
                Triple(lab[0], chroma, hue)
            }
            else -> Triple(encodeGamma(lr), encodeGamma(lg), encodeGamma(lb))
        }

    private fun inSrgbGamut(space: String, ch: DoubleArray): Boolean {
        val (r, g, b) = toLinearSrgb(space, ch).let { Triple(it[0], it[1], it[2]) }
        val eps = 1e-4
        return r >= -eps && r <= 1 + eps && g >= -eps && g <= 1 + eps && b >= -eps && b <= 1 + eps
    }

    /**
     * sRGB for the swatch. An out-of-gamut colour has its CHROMA reduced in OKLCH by
     * bisection rather than being clipped per channel, so the swatch keeps the hue and
     * lightness the author asked for instead of shifting hue.
     */
    private fun toSrgb(value: Channels): Srgb {
        val alpha = value.alpha?.let { clamp01(it) } ?: 1.0
        var space = value.space
        var ch = value.ch
        if (!inSrgbGamut(space, ch)) {
            val linear = toLinearSrgb(space, ch)
            val (l, chroma, hue) = fromLinearSrgb("oklch", linear[0], linear[1], linear[2])
            var lo = 0.0
            var hi = chroma
            repeat(20) {
                val mid = (lo + hi) / 2
                if (inSrgbGamut("oklch", doubleArrayOf(l, mid, hue))) lo = mid else hi = mid
            }
            space = "oklch"
            ch = doubleArrayOf(l, lo, hue)
        }
        val out = toLinearSrgb(space, ch)
        return Srgb(
            round6(clamp01(encodeGamma(out[0]))),
            round6(clamp01(encodeGamma(out[1]))),
            round6(clamp01(encodeGamma(out[2]))),
            round6(alpha),
        )
    }

    // ─── number formatting ────────────────────────────────────────────────────

    private fun hex2(v: Double): String = (clamp01(v) * 255).roundToInt().toString(16).padStart(2, '0')
    private fun byte(v: Double): String = (clamp01(v) * 255).roundToInt().toString()

    /**
     * Rounded to at most [decimals] places, with no trailing zeros (`0.5`, not `0.5000`).
     *
     * The precision above is deliberately generous, because trimming makes it free: a round
     * value still writes as `0.5`, while a colour that needs the digits keeps them. Being
     * stingy here is what breaks the round trip - at 4 decimals plus a 0.1° hue, picking pure
     * red and writing it as `oklch(…)` read back as #ff0001, since a hue that small an error
     * lands outside sRGB on the gamut boundary and comes back chroma-reduced.
     *
     * Locale.ROOT is not optional either: on a machine whose locale uses a decimal comma the
     * default formatter would write `0,5` into the file, and the parser would read two
     * channels where the author picked one colour.
     */
    private fun num(v: Double, decimals: Int): String {
        val s = String.format(java.util.Locale.ROOT, "%.${decimals}f", v)
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    }

    private fun pct(v: Double, decimals: Int): String = "${num(v * 100, decimals)}%"
}
