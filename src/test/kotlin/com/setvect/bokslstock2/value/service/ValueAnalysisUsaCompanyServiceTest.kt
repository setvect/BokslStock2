package com.setvect.bokslstock2.value.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")

class ValueAnalysisUsaCompanyServiceTest {
    @Autowired
    private lateinit var valueAnalysisUsaCompanyService: ValueAnalysisUsaCompanyService

    @Test
    fun analysis() {
        valueAnalysisUsaCompanyService.analysis()
    }
}