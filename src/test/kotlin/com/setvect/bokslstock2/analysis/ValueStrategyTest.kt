package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.value.service.ValueAnalysisService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class ValueStrategyTest {
    @Autowired
    private lateinit var valueAnalysisService: ValueAnalysisService

    @Test
    fun runTest(){
        valueAnalysisService.analysis()
    }


}