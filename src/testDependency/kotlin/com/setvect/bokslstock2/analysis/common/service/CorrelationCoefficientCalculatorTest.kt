package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
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
            StockCode.KODEX_200_069500,
            DateRange.maxRange
//            DateRange(LocalDate.of(2010, 1, 1), LocalDate.of(2010, 12, 31))
//            DateRange(LocalDate.of(2022, 1, 1), LocalDate.now())
        )
        println(calculateByMonth)
        println("끝.")
    }
}