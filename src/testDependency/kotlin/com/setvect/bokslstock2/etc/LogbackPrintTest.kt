package com.setvect.bokslstock2.etc

import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class LogbackPrintTest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun aa() {
        log.info("AAAAAAAAAA")
        println()
        println()
        println()
        println()
        println()
        // 인텔리제이의 소스 바로가기 링크는 어떨때 걸리는가?
        println("at com.setvect.bokslstock2.etc.LogbackPrintTest.aa(LogbackPrintTest.kt:17)")
        println("at LogbackPrintTest.aa(LogbackPrintTest.kt:17)")
        println("LogbackPrintTest.aa(LogbackPrintTest.kt:17)")
        println("AAAt.aa(LogbackPrintTest.kt:17)")
        println("BBB.aa(LogbackPrintTest.kt:17)")
        println("aa(LogbackPrintTest.kt:17)")
        println("(LogbackPrintTest.kt:17)")
        println("LogbackPrintTest.kt:17")
        Exception().printStackTrace()
    }

}