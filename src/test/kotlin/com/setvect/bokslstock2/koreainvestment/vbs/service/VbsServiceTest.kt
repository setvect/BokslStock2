package com.setvect.bokslstock2.koreainvestment.vbs.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
internal class VbsServiceTest {
    @Autowired
    private lateinit var vbsService: VbsService

    @Test
    fun report() {
        vbsService.report()
    }
}