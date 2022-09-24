package com.setvect.bokslstock2.koreainvestment.trade.repository

import com.setvect.bokslstock2.koreainvestment.trade.model.web.TradeSearchForm
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
internal class HistoryRepositoryTest {
    @Autowired
    private lateinit var historyRepository: HistoryRepository

    @Autowired
    private lateinit var tradeRepository: TradeRepository

    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun test() {
        val list = historyRepository.list(TradeSearchForm(), PageRequest.of(0, 2))

        log.info(list.toString())

        list.forEach {
            println(it)
        }


    }
}