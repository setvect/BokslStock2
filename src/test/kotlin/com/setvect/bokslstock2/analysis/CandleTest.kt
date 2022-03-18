package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.StockCode
import com.setvect.bokslstock2.index.repository.StockRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("local")
class CandleTest {
    @Autowired
    private lateinit var stockRepository: StockRepository

    @Test
    @Transactional
    fun 분봉값_비교_5_10() {
        val stock = stockRepository.findByCode(StockCode.CODE_KODEX_BANK_091170).get()
        val filterCandle = stock.candleList
            .filter { it.candleDateTime.hour == 9 && (it.candleDateTime.minute == 5 || it.candleDateTime.minute == 10) }
            .toList()

        var then10 = 0
        var equals10 = 0
        var diff = 0.0
        var i = 0
        while (i < filterCandle.size) {
            val candle5 = filterCandle[i]
            val candle10 = filterCandle[i + 1]
            if (candle5.candleDateTime.dayOfMonth != candle10.candleDateTime.dayOfMonth) {
                println("쌍이 맞지 않습니다. ${candle5.candleDateTime} ${candle10.candleDateTime}")
                i++
                continue
//                throw RuntimeException("쌍이 맞지 않습니다. ${candle5.candleDateTime} ${candle10.candleDateTime}")
            }

            if (candle5.openPrice < candle10.openPrice) {
                then10++
            } else if (candle5.openPrice == candle10.openPrice) {
                equals10++
            }

            diff += (candle10.openPrice - candle5.openPrice)
            i += 2
        }

        println("결과")
        println("5분보다 10분이 더 큰경우: $then10 ")
        println("5분과 10분이 같은경우: $equals10 ")
        println("5분보다 10분이 더 작은 경우: ${filterCandle.size / 2 - then10 - equals10} ")
        println("차이 금액: ${diff} ")

    }
}