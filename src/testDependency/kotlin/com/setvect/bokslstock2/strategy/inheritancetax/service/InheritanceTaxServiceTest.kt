package com.setvect.bokslstock2.strategy.inheritancetax.service

import com.setvect.bokslstock2.backtest.dart.service.DartStructuringService
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class InheritanceTaxServiceTest {
    @Autowired
    private lateinit var inheritanceTaxService: InheritanceTaxService
    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun analysis() {
        inheritanceTaxService.analysis()
    }
}