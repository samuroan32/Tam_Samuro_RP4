package com.twinscalev4.domain

object RoomJoinValidator {
    fun isValidName(value: String): Boolean = value.trim().length >= 2
    fun isValidRoom(value: String): Boolean = value.trim().length >= 3
    fun isValidSize(value: String): Boolean = value.toBigDecimalOrNull()?.let { it > java.math.BigDecimal.ZERO } == true
}
