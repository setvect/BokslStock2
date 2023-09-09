package com.setvect.bokslstock2.strategy.inheritancetax.service

import com.setvect.bokslstock2.strategy.inheritancetax.model.Quarter
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 상속세법 매매 전략 분석
 */
@SpringBootTest
@ActiveProfiles("test")
class InheritanceTaxServiceTest {
    @Autowired
    private lateinit var inheritanceTaxService: InheritanceTaxService
    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun analysis() {
        inheritanceTaxService.analysis(2023, Quarter.Q2)
    }
}