package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.setvect.bokslstock2.util.DateUtil
import java.time.LocalDateTime

data class TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("access_token_token_expired") val accessTokenTokenExpired: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Long
) {
    /**
     * @return 토큰 사용가능 여부
     */
    fun isExpired(): Boolean {
        return expired().isAfter(LocalDateTime.now())
    }

    /**
     * @return 토큰 종료 시간에서 -1시간
     */
    private fun expired(): LocalDateTime {
        return DateUtil.getLocalDateTime(accessTokenTokenExpired, DateUtil.yyyy_MM_dd_HH_mm_ss).minusHours(1)
    }

}