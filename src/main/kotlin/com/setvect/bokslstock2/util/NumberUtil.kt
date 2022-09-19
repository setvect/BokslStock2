package com.setvect.bokslstock2.util

object NumberUtil {
    fun comma(value: Number): String {
        return String.format("%,d", value)
    }

    /**
     * [value]
     * 1 -> 1.00%
     * 2.3 -> 2.30%
     */
    fun percent1(value: Number): String {
        return String.format("%,.2f%%", value)
    }

    /**
     * [value]
     * 1 -> 100.00%
     * 2.3 -> 230.00%
     */
    fun percent(value: Number): String {
        return String.format("%,.2f%%", value)
    }

}