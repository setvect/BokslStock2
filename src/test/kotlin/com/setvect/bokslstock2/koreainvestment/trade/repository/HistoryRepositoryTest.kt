package com.setvect.bokslstock2.koreainvestment.trade.repository

import com.setvect.bokslstock2.koreainvestment.trade.model.web.TradeSearchForm
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("local")
internal class HistoryRepositoryTest {
    @Autowired
    private lateinit var historyRepository: HistoryRepository

    @Test
    fun test() {
        println("############## $historyRepository")
        val list = historyRepository.list(TradeSearchForm(LocalDateTime.now(), LocalDateTime.now()), PageRequest.of(1, 10))

        list.forEach {
            println(it)
        }
    }
}