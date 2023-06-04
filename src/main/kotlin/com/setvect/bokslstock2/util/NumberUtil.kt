package com.setvect.bokslstock2.util

object NumberUtil {
    fun comma(value: Number): String {
        return String.format("%,d", value)
    }

    /**
     * [value]
     * 100 -> 100.00%
     * 230.23 -> 230.23%
     */
    fun percent(value: Number): String {
        return String.format("%,.2f%%", value)
    }

    /**
     * 단위수가 적용된 문자열은 숫자로 변환
     * 10B -> 10,000,000,000
     * 10M -> 10,000,000
     * 10K -> 10,000
     */
    fun unitToNumber(marketCap: String): Double {
        val number = marketCap.substring(0, marketCap.length - 1).toDoubleOrNull() ?: 0.0

        return when (marketCap.last()) {
            'B' -> number * 1_000_000_000
            'M' -> number * 1_000_000
            'K' -> number * 1_000
            else -> number
        }
    }

    /**
     * 퍼센트 표현을 숫자로 변환
     * 10% -> 0.1
     */
    fun percentToNumber(percentage: String): Double {
        val number = percentage.removeSuffix("%").toDoubleOrNull() ?: 0.0
        return number / 100
    }

    /**
     * 콤마가 들어가 문자를 숫자로 변환
     * 10,000 -> 10000
     */
    fun commaToNumber(numberString: String): Int {
        val noCommas = numberString.replace(",", "")
        return noCommas.toIntOrNull() ?: 0
    }
}