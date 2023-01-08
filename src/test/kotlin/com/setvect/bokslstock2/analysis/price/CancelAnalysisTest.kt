package com.setvect.bokslstock2.analysis.price

import com.setvect.bokslstock2.index.repository.CandleRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class CancelAnalysisTest {
    @Autowired
    private lateinit var candleRepository: CandleRepository


    @Test
    @DisplayName("갭 상승일 때 매도 타이밍 테스트 ")
    fun test1(){
//        candleRepository.findByCandleDateTimeBetween()
    }

}