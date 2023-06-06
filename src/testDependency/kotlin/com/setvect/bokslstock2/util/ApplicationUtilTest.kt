package com.setvect.bokslstock2.util

import org.assertj.core.api.Assertions
import org.assertj.core.data.Offset
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

    @Test
    fun getBuyCash() {
        var buyCash = ApplicationUtil.getBuyCash(0.0, 10_000_000.0, 0.1, 0.9)
        System.out.printf("%,.0f\n", buyCash)
        Assertions.assertThat(buyCash).isCloseTo(900_000.0, Offset.offset(0.001))

        buyCash = ApplicationUtil.getBuyCash(0.5, 7_500_000.0, 0.2, 0.5)
        System.out.printf("%,.0f\n", buyCash)
        Assertions.assertThat(buyCash).isCloseTo(1_000_000.0, Offset.offset(0.001))

        buyCash = ApplicationUtil.getBuyCash(0.1, 9_000_000.0, 0.3, 1.0)
        System.out.printf("%,.0f\n", buyCash)
        Assertions.assertThat(buyCash).isCloseTo(3_000_000.0, Offset.offset(0.001))
    }

}