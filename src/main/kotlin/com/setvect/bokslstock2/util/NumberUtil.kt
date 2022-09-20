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

}