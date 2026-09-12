package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.koreainvestment.trade.model.response.TokenResponse
import com.setvect.bokslstock2.util.DateUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

private const val TOKEN_REQUEST_MAX_ATTEMPTS = 3
private const val TOKEN_REQUEST_RETRY_INTERVAL_SECONDS = 65L

internal fun isTokenIssuanceRateLimitError(exception: Exception): Boolean {
    return exception is HttpClientErrorException &&
            exception.statusCode == HttpStatus.FORBIDDEN &&
            exception.responseBodyAsString.contains("EGW00133")
}

internal fun <T> retryTokenRequest(
    maxAttempts: Int,
    retryIntervalSeconds: Long,
    request: () -> T,
    onRetry: (Int, Exception) -> Unit = { _, _ -> },
    sleeper: (Long) -> Unit = { TimeUnit.SECONDS.sleep(it) },
): T {
    require(maxAttempts > 0) { "최대 시도 횟수는 1 이상이어야 합니다." }

    for (attempt in 1..maxAttempts) {
        try {
            return request()
        } catch (e: Exception) {
            if (!isTokenIssuanceRateLimitError(e) || attempt == maxAttempts) {
                throw e
            }
            onRetry(attempt, e)
            sleeper(retryIntervalSeconds)
        }
    }

    throw IllegalStateException("접근토큰 발급 재시도 상태가 올바르지 않습니다.")
}

@Service
class TokenService(
    private val stockClientService: StockClientService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private var token: TokenResponse = TokenResponse("", DateUtil.currentDateTime(DateUtil.yyyy_MM_dd_HH_mm_ss), "", 0L)
    private var currentDate: LocalDate = LocalDate.now().minusDays(1)

    @Synchronized
    fun getAccessToken(): String {
        if (currentDate != LocalDate.now()) {
            loadToken()
            currentDate = LocalDate.now()
        }

        return token.accessToken
    }

    private fun loadToken() {
        token = retryTokenRequest(
            maxAttempts = TOKEN_REQUEST_MAX_ATTEMPTS,
            retryIntervalSeconds = TOKEN_REQUEST_RETRY_INTERVAL_SECONDS,
            request = stockClientService::requestToken,
            onRetry = { attempt, _ ->
                log.warn(
                    "접근토큰 발급 제한({}/{}). {}초 후 재시도",
                    attempt,
                    TOKEN_REQUEST_MAX_ATTEMPTS,
                    TOKEN_REQUEST_RETRY_INTERVAL_SECONDS
                )
            }
        )
        log.info("접근토큰 로드 완료")
    }
}
