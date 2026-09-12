package com.setvect.bokslstock2.koreainvestment.vbs.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SingleExecutionGuardTest {

    @Test
    fun `실행 중 예외가 발생해도 다음 실행을 허용한다`() {
        val guard = SingleExecutionGuard()

        assertThrows(IllegalStateException::class.java) {
            guard.executeIfIdle {
                throw IllegalStateException("시작 실패")
            }
        }

        var executed = false
        val accepted = guard.executeIfIdle {
            executed = true
        }

        assertThat(accepted).isTrue()
        assertThat(executed).isTrue()
    }

    @Test
    fun `이미 실행 중이면 중복 실행을 거부한다`() {
        val guard = SingleExecutionGuard()
        var nestedExecuted = false

        val firstAccepted = guard.executeIfIdle {
            val nestedAccepted = guard.executeIfIdle {
                nestedExecuted = true
            }
            assertThat(nestedAccepted).isFalse()
        }

        assertThat(firstAccepted).isTrue()
        assertThat(nestedExecuted).isFalse()
    }
}
