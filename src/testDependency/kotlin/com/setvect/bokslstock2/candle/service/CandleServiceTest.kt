package com.setvect.bokslstock2.candle.service
import com.setvect.bokslstock2.index.repository.CandleRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class CandleServiceTest {
    @Autowired
    private lateinit var candleRepository: CandleRepository
    @Test
    fun test() {
        println(candleRepository)
        println("DDDDDDDDDDDDDDDDDDDDDDDD")
    }
}