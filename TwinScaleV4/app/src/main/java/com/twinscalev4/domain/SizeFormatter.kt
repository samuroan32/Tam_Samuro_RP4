package com.twinscalev4.domain

import java.math.BigDecimal
import java.math.RoundingMode

object SizeFormatter {

    private data class UnitScale(val symbol: String, val meters: BigDecimal)

    private val units = listOf(
        UnitScale("пм", BigDecimal("1e-12")),
        UnitScale("нм", BigDecimal("1e-9")),
        UnitScale("мкм", BigDecimal("1e-6")),
        UnitScale("мм", BigDecimal("1e-3")),
        UnitScale("см", BigDecimal("1e-2")),
        UnitScale("м", BigDecimal("1")),
        UnitScale("км", BigDecimal("1e3")),
        UnitScale("а.е.", BigDecimal("1.495978707e11"))
    )

    fun formatMeters(rawMeters: String): String {
        val value = rawMeters.toBigDecimalOrNull() ?: BigDecimal.ONE

        val selected = units.lastOrNull { value >= it.meters } ?: units.first()
        val converted = value.divide(selected.meters, 6, RoundingMode.HALF_UP)

        val text = when {
            converted >= BigDecimal("100") -> converted.setScale(1, RoundingMode.HALF_UP)
            converted >= BigDecimal("10") -> converted.setScale(2, RoundingMode.HALF_UP)
            else -> converted.setScale(3, RoundingMode.HALF_UP)
        }.stripTrailingZeros().toPlainString()

        return "$text ${selected.symbol}"
    }

    fun ratioText(selfRaw: String, partnerRaw: String): String {
        val self = selfRaw.toBigDecimalOrNull() ?: BigDecimal.ONE
        val partner = partnerRaw.toBigDecimalOrNull() ?: BigDecimal.ONE

        if (self == BigDecimal.ZERO || partner == BigDecimal.ZERO) return "Сравнение недоступно"

        return if (self >= partner) {
            val ratio = self.divide(partner, 2, RoundingMode.HALF_UP)
            "Вы больше в ${ratio.stripTrailingZeros().toPlainString()}×"
        } else {
            val ratio = partner.divide(self, 2, RoundingMode.HALF_UP)
            "Партнёр больше в ${ratio.stripTrailingZeros().toPlainString()}×"
        }
    }
}
