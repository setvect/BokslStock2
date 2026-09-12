package com.setvect.bokslstock2.koreainvestment.trade.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.nio.charset.StandardCharsets

class TokenRequestRetryTest {

    @Test
    fun `토큰 발급 제한이면 대기 후 재시도한다`() {
        var attempts = 0
        val sleepIntervals = mutableListOf<Long>()

        val result = retryTokenRequest(
            maxAttempts = 3,
            retryIntervalSeconds = 65,
            request = {
                attempts++
                if (attempts < 3) {
                    throw tokenRateLimitException()
                }
                "access-token"
            },
            sleeper = sleepIntervals::add,
        )

        assertThat(result).isEqualTo("access-token")
        assertThat(attempts).isEqualTo(3)
        assertThat(sleepIntervals).containsExactly(65, 65)
    }

    @Test
    fun `토큰 발급 제한이 계속되면 최대 횟수까지만 시도한다`() {
        var attempts = 0
        val sleepIntervals = mutableListOf<Long>()

        assertThrows(HttpClientErrorException.Forbidden::class.java) {
            retryTokenRequest(
                maxAttempts = 3,
                retryIntervalSeconds = 65,
                request = {
                    attempts++
                    throw tokenRateLimitException()
                },
                sleeper = sleepIntervals::add,
            )
        }

        assertThat(attempts).isEqualTo(3)
        assertThat(sleepIntervals).containsExactly(65, 65)
    }

    @Test
    fun `발급 제한이 아닌 오류는 재시도하지 않는다`() {
        var attempts = 0
        val sleepIntervals = mutableListOf<Long>()

        assertThrows(HttpClientErrorException.Forbidden::class.java) {
            retryTokenRequest(
                maxAttempts = 3,
                retryIntervalSeconds = 65,
                request = {
                    attempts++
                    throw forbiddenException("OTHER_ERROR")
                },
                sleeper = sleepIntervals::add,
            )
        }

        assertThat(attempts).isEqualTo(1)
        assertThat(sleepIntervals).isEmpty()
    }

    private fun tokenRateLimitException() = forbiddenException("EGW00133")

    private fun forbiddenException(errorCode: String): HttpClientErrorException {
        val body = """{"error_code":"$errorCode"}""".toByteArray(StandardCharsets.UTF_8)
        return HttpClientErrorException.create(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            HttpHeaders.EMPTY,
            body,
            StandardCharsets.UTF_8,
        )
    }
}
