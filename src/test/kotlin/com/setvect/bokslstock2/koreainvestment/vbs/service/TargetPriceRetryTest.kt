package com.setvect.bokslstock2.koreainvestment.vbs.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TargetPriceRetryTest {

    @Test
    fun `가격정보가 준비되면 재시도 후 결과를 반환한다`() {
        var attempts = 0
        val failures = mutableListOf<Int>()
        val sleepIntervals = mutableListOf<Long>()

        val result = retryTargetPriceCalculation(
            maxAttempts = 10,
            retryIntervalSeconds = 10,
            calculation = {
                attempts++
                if (attempts < 3) {
                    throw TargetPriceNotReadyException("가격정보 준비 중")
                }
                4255
            },
            onFailure = { attempt, _ -> failures.add(attempt) },
            sleeper = { sleepIntervals.add(it) },
        )

        assertThat(result).isEqualTo(4255)
        assertThat(attempts).isEqualTo(3)
        assertThat(failures).containsExactly(1, 2)
        assertThat(sleepIntervals).containsExactly(10, 10)
    }

    @Test
    fun `가격정보가 계속 준비되지 않으면 열 번 시도 후 실패한다`() {
        var attempts = 0
        val sleepIntervals = mutableListOf<Long>()

        assertThrows(TargetPriceNotReadyException::class.java) {
            retryTargetPriceCalculation(
                maxAttempts = 10,
                retryIntervalSeconds = 10,
                calculation = {
                    attempts++
                    throw TargetPriceNotReadyException("가격정보 준비 중")
                },
                sleeper = { sleepIntervals.add(it) },
            )
        }

        assertThat(attempts).isEqualTo(10)
        assertThat(sleepIntervals).hasSize(9)
        assertThat(sleepIntervals).allMatch { it == 10L }
    }

    @Test
    fun `가격정보 준비 오류가 아니면 재시도하지 않는다`() {
        var attempts = 0
        val sleepIntervals = mutableListOf<Long>()

        assertThrows(IllegalStateException::class.java) {
            retryTargetPriceCalculation(
                maxAttempts = 10,
                retryIntervalSeconds = 10,
                calculation = {
                    attempts++
                    throw IllegalStateException("설정 오류")
                },
                sleeper = { sleepIntervals.add(it) },
            )
        }

        assertThat(attempts).isEqualTo(1)
        assertThat(sleepIntervals).isEmpty()
    }
}
