package com.setvect.bokslstock2.util

import org.junit.jupiter.api.Test

internal class ApplicationUtilTest {

    @Test
    fun getCagr() {
        val cagr = ApplicationUtil.getCagr(2080.0, 2444.0, 15 * 265)
        println("복리: ${cagr * 100}% ")
    }

    @Test
    fun calcPriceYieldTest() {
        val calcPriceYield = ApplicationUtil.calcPriceYield(listOf(1.0, 1.1, 1.0))
        calcPriceYield.forEach { println(it) }
    }

}