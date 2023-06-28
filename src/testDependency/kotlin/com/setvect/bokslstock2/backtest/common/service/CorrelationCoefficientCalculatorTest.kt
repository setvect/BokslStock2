package com.setvect.bokslstock2.backtest.common.service

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.util.DateRange
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class CorrelationCoefficientCalculatorTest {

    @Autowired
    private lateinit var correlationCoefficientCalculator: CorrelationCoefficientCalculator

    @Test
    fun calculateByMonth() {
//        val calculateByMonth = correlationCoefficientCalculator.calculateByMonth(
//            StockCode.OS_CODE_SPY,
//            StockCode.OS_CODE_TLT,
////            DateRange.maxRange
////            DateRange(LocalDate.of(2010, 1, 1), LocalDate.of(2010, 12, 31))
//            DateRange(LocalDate.of(2022, 1, 1), LocalDate.now())
//        )

        val calculateByMonth = correlationCoefficientCalculator.calculateByMonth(
            StockCode.EXCHANGE_DOLLAR,
            StockCode.TIGER_USA_TREASURY_BOND_305080,
            DateRange.maxRange
//            DateRange(LocalDate.of(2010, 1, 1), LocalDate.of(2010, 12, 31))
//            DateRange(LocalDate.of(2022, 1, 1), LocalDate.now())
        )
        println(calculateByMonth)
        println("끝.")
    }

    @Test
    fun calculateByMonthMatrix() {
        val stockCodeList = listOf(
            StockCode.OS_CODE_TQQQ,
            StockCode.OS_CODE_TMF,
            StockCode.OS_CODE_UGL,
            StockCode.OS_CODE_SHY,
            )
        val calculateByMonthMatrix =
            correlationCoefficientCalculator.calculateByMonthMatrix(stockCodeList, DateRange.maxRange)

        for (i in calculateByMonthMatrix.indices) {
            for (j in calculateByMonthMatrix[i].indices) {
                println("${calculateByMonthMatrix[i][j]} ")
            }
            println()
        }
    }
}