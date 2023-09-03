package com.setvect.bokslstock2.backtest.dart.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 재무제표 데이터 DB에 저장
 */
@SpringBootTest
@ActiveProfiles("test")
class DartDataStoreServiceTest {
    @Autowired
    private lateinit var dartDataStoreService: DartDataStoreService

    @Test
    fun loadFinancial() {
        dartDataStoreService.loadFinancial()
    }
}