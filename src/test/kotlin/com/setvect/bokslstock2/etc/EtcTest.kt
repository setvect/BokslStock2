package com.setvect.bokslstock2.etc

import com.setvect.bokslstock2.util.DateUtil
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class EtcTest {
    @Test
    fun test() {
        val l = LocalDate.of(1994, 1, 1)
        val unix = l.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
        println(unix)
    }

    @Test
    fun test1() {
        var convertDateOfMonday = DateUtil.convertDateOfMonday(LocalDateTime.of(2022, 8, 12, 0, 0))
        Assertions.assertThat(convertDateOfMonday.year).isEqualTo(2022)
        Assertions.assertThat(convertDateOfMonday.monthValue).isEqualTo(8)
        Assertions.assertThat(convertDateOfMonday.dayOfMonth).isEqualTo(8)

        convertDateOfMonday = DateUtil.convertDateOfMonday(LocalDateTime.of(2022, 8, 8, 0, 0))
        Assertions.assertThat(convertDateOfMonday.year).isEqualTo(2022)
        Assertions.assertThat(convertDateOfMonday.monthValue).isEqualTo(8)
        Assertions.assertThat(convertDateOfMonday.dayOfMonth).isEqualTo(8)

        convertDateOfMonday = DateUtil.convertDateOfMonday(LocalDateTime.of(2022, 7, 31, 0, 0))
        Assertions.assertThat(convertDateOfMonday.year).isEqualTo(2022)
        Assertions.assertThat(convertDateOfMonday.monthValue).isEqualTo(7)
        Assertions.assertThat(convertDateOfMonday.dayOfMonth).isEqualTo(25)

        convertDateOfMonday = DateUtil.convertDateOfFriday(LocalDateTime.of(2022, 8, 11, 0, 0))
        Assertions.assertThat(convertDateOfMonday.year).isEqualTo(2022)
        Assertions.assertThat(convertDateOfMonday.monthValue).isEqualTo(8)
        Assertions.assertThat(convertDateOfMonday.dayOfMonth).isEqualTo(12)
    }
}