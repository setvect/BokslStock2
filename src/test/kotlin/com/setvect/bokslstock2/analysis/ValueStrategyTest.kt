package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.value.service.ValueAnalysisKoreanCompanyService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class ValueStrategyTest {
    @Autowired
    private lateinit var valueAnalysisKoreanCompanyService: ValueAnalysisKoreanCompanyService

    @Test
    fun runTest() {
        valueAnalysisKoreanCompanyService.analysis()
    }


}