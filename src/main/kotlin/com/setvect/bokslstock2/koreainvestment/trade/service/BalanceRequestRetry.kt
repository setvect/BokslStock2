package com.setvect.bokslstock2.koreainvestment.trade.service

import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import java.util.concurrent.TimeUnit

private val BALANCE_RETRY_INTERVALS_SECONDS = listOf(10L, 30L, 60L, 120L, 180L)

/** 최초 요청에 더해 일시적인 조회 실패를 최대 5번 재시도한다. 주문 요청에는 사용하지 않는다. */
internal fun <T> retryBalanceRequest(
    request: () -> T,
    onRetry: (Int, Long, Exception) -> Unit = { _, _, _ -> },
    onFailure: (Int, Exception) -> Unit = { _, _ -> },
    sleeper: (Long) -> Unit = { TimeUnit.SECONDS.sleep(it) },
): T {
    for (attempt in 0..BALANCE_RETRY_INTERVALS_SECONDS.size) {
        try {
            return request()
        } catch (e: Exception) {
            val retryable = when (e) {
                is ResourceAccessException -> true
                is RestClientResponseException -> e.rawStatusCode == 429 || e.rawStatusCode in 500..599
                else -> false
            }
            if (!retryable || attempt == BALANCE_RETRY_INTERVALS_SECONDS.size) {
                onFailure(attempt + 1, e)
                throw e
            }

            val interval = BALANCE_RETRY_INTERVALS_SECONDS[attempt]
            onRetry(attempt + 1, interval, e)
            sleeper(interval)
        }
    }

    throw IllegalStateException("잔고 조회 재시도 상태가 올바르지 않습니다.")
}
