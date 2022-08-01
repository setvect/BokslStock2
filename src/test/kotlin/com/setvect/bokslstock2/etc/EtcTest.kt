package com.setvect.bokslstock2.etc

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class EtcTest {
    @Test
    fun test() {
        val l = LocalDate.of(1994, 1, 1)
        val unix = l.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
        println(unix)
    }
}