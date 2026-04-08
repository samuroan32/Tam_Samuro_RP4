package com.twinscalev4.domain

import com.twinscalev4.data.GrowthMode
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.random.Random

object SizeMath {

    private val mc = MathContext(50, RoundingMode.HALF_UP)
    private val minMeters = BigDecimal("1e-12")
    private val maxMeters = BigDecimal("1.495978707e11")

    fun nextSize(current: BigDecimal, mode: GrowthMode, grow: Boolean): BigDecimal {
        val safeCurrent = current.coerceAtLeast(minMeters)

        val logFactor = safeCurrent.stripTrailingZeros().precision().coerceAtLeast(1) / 20.0
        val randomFactor = Random.nextDouble(0.75, 1.35)
        val step = mode.basePercent * (1.0 + logFactor) * randomFactor

        val multiplier = if (grow) {
            BigDecimal.ONE.add(BigDecimal(step, mc), mc)
        } else {
            BigDecimal.ONE.subtract(BigDecimal(step, mc).coerceAtMost(BigDecimal("0.95")), mc)
        }

        return safeCurrent.multiply(multiplier, mc)
            .coerceAtLeast(minMeters)
            .coerceAtMost(maxMeters)
    }
}

private fun BigDecimal.coerceAtLeast(min: BigDecimal): BigDecimal = if (this < min) min else this
private fun BigDecimal.coerceAtMost(max: BigDecimal): BigDecimal = if (this > max) max else this
