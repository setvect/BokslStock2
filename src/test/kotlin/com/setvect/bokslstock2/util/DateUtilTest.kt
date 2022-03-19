package com.setvect.bokslstock2.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class DateUtilTest {

    @Test
    fun isBetween() {
        var between = DateUtil.isBetween(
            DateUtil.getLocalDateTime("2021-01-01T00:00:00"),
            DateUtil.getLocalDateTime("2020-01-01T00:00:00"),
            DateUtil.getLocalDateTime("2022-01-01T00:00:00")
        )
        Assertions.assertThat(between).isTrue

        between = DateUtil.isBetween(
            DateUtil.getLocalDateTime("2020-01-01T00:00:00"),
            DateUtil.getLocalDateTime("2020-01-01T00:00:00"),
            DateUtil.getLocalDateTime("2022-01-01T00:00:00")
        )
        Assertions.assertThat(between).isTrue

        between = DateUtil.isBetween(
            DateUtil.getLocalDateTime("2020-01-01T00:00:00"),
            DateUtil.getLocalDateTime("2020-01-01T00:00:01"),
            DateUtil.getLocalDateTime("2022-01-01T00:00:00")
        )
        Assertions.assertThat(between).isFalse
    }
}