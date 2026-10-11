package com.setvect.bokslstock2.koreainvestment.trade.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets

class BalanceRequestRetryTest {

    @Test
    fun `최초 조회가 성공하면 대기하지 않는다`() {
        val intervals = mutableListOf<Long>()
        val result = retryBalanceRequest(request = { "balance" }, sleeper = intervals::add)

        assertThat(result).isEqualTo("balance")
        assertThat(intervals).isEmpty()
    }

    @Test
    fun `호출 제한 후 성공하면 즉시 재시도를 멈춘다`() {
        var attempts = 0
        val intervals = mutableListOf<Long>()
        val retries = mutableListOf<Int>()
        val failures = mutableListOf<Int>()
        val result = retryBalanceRequest(
            request = {
                attempts++
                if (attempts < 3) throw rateLimitException()
                "balance"
            },
            onRetry = { retry, _, _ -> retries.add(retry) },
            onFailure = { attempt, _ -> failures.add(attempt) },
            sleeper = intervals::add,
        )

        assertThat(result).isEqualTo("balance")
        assertThat(attempts).isEqualTo(3)
        assertThat(intervals).containsExactly(10L, 30L)
        assertThat(retries).containsExactly(1, 2)
        assertThat(failures).isEmpty()
    }

    @Test
    fun `계속 실패하면 지정된 간격으로 다섯 번 재시도하고 마지막 오류를 전달한다`() {
        var attempts = 0
        val intervals = mutableListOf<Long>()
        val failures = mutableListOf<Int>()
        val errors = (1..6).map { rateLimitException() }
        val thrown = assertThrows(HttpServerErrorException::class.java) {
            retryBalanceRequest(
                request = { throw errors[attempts++] },
                onFailure = { attempt, _ -> failures.add(attempt) },
                sleeper = intervals::add,
            )
        }

        assertThat(attempts).isEqualTo(6)
        assertThat(intervals).containsExactly(10L, 30L, 60L, 120L, 180L)
        assertThat(failures).containsExactly(6)
        assertThat(thrown).isSameAs(errors.last())
    }

    @Test
    fun `마지막 다섯 번째 재시도에서도 성공할 수 있다`() {
        var attempts = 0
        val intervals = mutableListOf<Long>()
        val result = retryBalanceRequest(
            request = {
                attempts++
                if (attempts < 6) throw rateLimitException()
                "balance"
            },
            sleeper = intervals::add,
        )

        assertThat(result).isEqualTo("balance")
        assertThat(attempts).isEqualTo(6)
        assertThat(intervals).containsExactly(10L, 30L, 60L, 120L, 180L)
    }

    @Test
    fun `HTTP 429와 서버 장애 및 통신 오류도 재시도한다`() {
        val errors = listOf(
            HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS),
            HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE),
            ResourceAccessException("timeout", SocketTimeoutException()),
        )
        errors.forEach { error ->
            var attempts = 0
            val intervals = mutableListOf<Long>()
            val result = retryBalanceRequest(
                request = {
                    attempts++
                    if (attempts == 1) throw error
                    "balance"
                },
                sleeper = intervals::add,
            )

            assertThat(result).isEqualTo("balance")
            assertThat(attempts).isEqualTo(2)
            assertThat(intervals).containsExactly(10L)
        }
    }

    @Test
    fun `잘못된 요청이나 인증 오류 및 프로그램 오류는 즉시 전달한다`() {
        val errors = listOf(
            HttpClientErrorException(HttpStatus.BAD_REQUEST),
            HttpClientErrorException(HttpStatus.UNAUTHORIZED),
            HttpClientErrorException(HttpStatus.FORBIDDEN),
            IllegalStateException("invalid state"),
        )
        errors.forEach { error ->
            var attempts = 0
            val intervals = mutableListOf<Long>()
            val failures = mutableListOf<Int>()
            val thrown = assertThrows(Exception::class.java) {
                retryBalanceRequest(
                    request = {
                        attempts++
                        throw error
                    },
                    onFailure = { attempt, _ -> failures.add(attempt) },
                    sleeper = intervals::add,
                )
            }

            assertThat(thrown).isSameAs(error)
            assertThat(attempts).isEqualTo(1)
            assertThat(intervals).isEmpty()
            assertThat(failures).containsExactly(1)
        }
    }

    @Test
    fun `대기 중 인터럽트가 발생하면 추가 조회하지 않는다`() {
        var attempts = 0
        assertThrows(InterruptedException::class.java) {
            retryBalanceRequest(
                request = {
                    attempts++
                    throw rateLimitException()
                },
                sleeper = { throw InterruptedException() },
            )
        }

        assertThat(attempts).isEqualTo(1)
    }

    private fun rateLimitException(): HttpServerErrorException {
        val body = """{"rt_cd":"1","msg_cd":"EGW00215"}""".toByteArray(StandardCharsets.UTF_8)
        return HttpServerErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            HttpHeaders.EMPTY,
            body,
            StandardCharsets.UTF_8,
        )
    }
}
