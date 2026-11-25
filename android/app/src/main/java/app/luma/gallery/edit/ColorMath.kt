package app.luma.gallery.edit

/**
 * «Ajustes», «Filtros» and «Estudio IA › Mejorar» as colour matrices (4×5, row by row, the format Android's ColorMatrix
 * and Compose's use: R' = a·R + b·G + c·B + d·A + e, colours 0..255). Pure, with tests.
 */

/** «Ajustes»: each from -1 to 1 (0 = untouched). */
data class Adjust(val brightness: Float = 0f, val contrast: Float = 0f, val saturation: Float = 0f, val warmth: Float = 0f) {
    val untouched get() = this == Adjust()
}

/** The sliders of «Ajustes», in order. */
enum class AdjustKind { BRIGHTNESS, CONTRAST, SATURATION, WARMTH }

/** «Filtros». */
enum class Filter { NONE, VIVID, MONO, SEPIA, COOL, WARM, VINTAGE, NOIR }

object ColorMath {
    val IDENTITY = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)

    fun get(adjust: Adjust, kind: AdjustKind) = when (kind) {
        AdjustKind.BRIGHTNESS -> adjust.brightness; AdjustKind.CONTRAST -> adjust.contrast
        AdjustKind.SATURATION -> adjust.saturation; AdjustKind.WARMTH -> adjust.warmth
    }

    fun set(adjust: Adjust, kind: AdjustKind, value: Float): Adjust {
        val v = value.coerceIn(-1f, 1f)
        return when (kind) {
            AdjustKind.BRIGHTNESS -> adjust.copy(brightness = v); AdjustKind.CONTRAST -> adjust.copy(contrast = v)
            AdjustKind.SATURATION -> adjust.copy(saturation = v); AdjustKind.WARMTH -> adjust.copy(warmth = v)
        }
    }

    fun adjust(a: Adjust): FloatArray =
        concat(concat(concat(brightness(a.brightness * 70f), contrast(1f + a.contrast * 0.8f)), saturation(1f + a.saturation)), warmth(a.warmth * 30f))

    fun filter(f: Filter): FloatArray = when (f) {
        Filter.NONE -> IDENTITY
        Filter.VIVID -> concat(saturation(1.4f), contrast(1.12f))
        Filter.MONO -> saturation(0f)
        Filter.SEPIA -> concat(saturation(0f), floatArrayOf(1.07f, 0f, 0f, 0f, 22f, 0f, 0.95f, 0f, 0f, 8f, 0f, 0f, 0.78f, 0f, -8f, 0f, 0f, 0f, 1f, 0f))
        Filter.COOL -> warmth(-28f)
        Filter.WARM -> warmth(26f)
        Filter.VINTAGE -> concat(concat(saturation(0.7f), contrast(0.86f)), concat(warmth(18f), brightness(10f)))
        Filter.NOIR -> concat(saturation(0f), contrast(1.45f))
    }

    /** Adjust first, then the filter on top. */
    fun combined(adjust: Adjust, filter: Filter): FloatArray = concat(adjust(adjust), filter(filter))

    /**
     * «Mejorar»: stretches the light so the darkest 1 % becomes black and the brightest 1 % white (at most ×2.2), with a
     * little more colour. [histogram] counts the pixels of each brightness 0..255.
     */
    fun autoEnhance(histogram: IntArray): FloatArray {
        val total = histogram.sum().takeIf { it > 0 } ?: return IDENTITY
        fun percentile(p: Double): Int { var sum = 0L; for (i in 0..255) { sum += histogram[i]; if (sum >= total * p) return i }; return 255 }
        val lo = percentile(0.01); val hi = percentile(0.99)
        val scale = if (hi - lo < 8) 1f else (255f / (hi - lo)).coerceAtMost(2.2f)
        val offset = -lo * scale
        val levels = floatArrayOf(scale, 0f, 0f, 0f, offset, 0f, scale, 0f, 0f, offset, 0f, 0f, scale, 0f, offset, 0f, 0f, 0f, 1f, 0f)
        return concat(levels, saturation(1.12f))
    }

    /** [first], then [second] (as Android's ColorMatrix.postConcat). */
    fun concat(first: FloatArray, second: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0..3) {
            for (col in 0..4) {
                var v = if (col == 4) second[row * 5 + 4] else 0f
                for (k in 0..3) v += second[row * 5 + k] * first[k * 5 + col]
                out[row * 5 + col] = v
            }
        }
        return out
    }

    /** One 0xAARRGGBB colour through [m] (for tests and small previews). */
    fun apply(m: FloatArray, argb: Int): Int {
        val c = intArrayOf((argb shr 16) and 255, (argb shr 8) and 255, argb and 255, (argb ushr 24) and 255)
        fun ch(row: Int) = (m[row * 5] * c[0] + m[row * 5 + 1] * c[1] + m[row * 5 + 2] * c[2] + m[row * 5 + 3] * c[3] + m[row * 5 + 4])
            .toInt().coerceIn(0, 255)
        return (ch(3) shl 24) or (ch(0) shl 16) or (ch(1) shl 8) or ch(2)
    }

    private fun brightness(offset: Float) = floatArrayOf(1f, 0f, 0f, 0f, offset, 0f, 1f, 0f, 0f, offset, 0f, 0f, 1f, 0f, offset, 0f, 0f, 0f, 1f, 0f)

    private fun contrast(scale: Float): FloatArray {
        val t = 128f * (1f - scale)
        return floatArrayOf(scale, 0f, 0f, 0f, t, 0f, scale, 0f, 0f, t, 0f, 0f, scale, 0f, t, 0f, 0f, 0f, 1f, 0f)
    }

    /** Rec. 709 weights: 0 = grey, 1 = untouched. */
    private fun saturation(s: Float): FloatArray {
        val r = 0.2126f * (1 - s); val g = 0.7152f * (1 - s); val b = 0.0722f * (1 - s)
        return floatArrayOf(r + s, g, b, 0f, 0f, r, g + s, b, 0f, 0f, r, g, b + s, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    private fun warmth(amount: Float) = floatArrayOf(1f, 0f, 0f, 0f, amount, 0f, 1f, 0f, 0f, amount * 0.15f, 0f, 0f, 1f, 0f, -amount, 0f, 0f, 0f, 1f, 0f)
}

/** Work on raw pixels (0xAARRGGBB, row by row). Pure, with tests. */
object Pixels {
    /** How many pixels have each brightness 0..255 (looking at every [step]-th pixel). */
    fun histogram(pixels: IntArray, step: Int = 1): IntArray {
        val h = IntArray(256)
        var i = 0
        while (i < pixels.size) { h[luma(pixels[i])]++; i += step }
        return h
    }

    fun luma(argb: Int): Int = ((argb shr 16 and 255) * 54 + (argb shr 8 and 255) * 183 + (argb and 255) * 19) shr 8

    /** «Nitidez»: a 3×3 sharpening of strength [amount] (0..1); the border stays as it was. */
    fun sharpen(pixels: IntArray, w: Int, h: Int, amount: Float): IntArray {
        val out = pixels.copyOf()
        if (w < 3 || h < 3 || amount <= 0f) return out
        val center = 1f + 4f * amount
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            val n = intArrayOf(pixels[i - w], pixels[i + w], pixels[i - 1], pixels[i + 1])
            fun ch(shift: Int): Int {
                val around = n.sumOf { (it shr shift) and 255 }
                return (center * ((pixels[i] shr shift) and 255) - amount * around).toInt().coerceIn(0, 255)
            }
            out[i] = (pixels[i] and -0x1000000) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }
        return out
    }
}
