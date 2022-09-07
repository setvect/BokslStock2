package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.model.HashResponse
import com.setvect.bokslstock2.koreainvestment.trade.model.TokenRequest
import com.setvect.bokslstock2.koreainvestment.trade.model.TokenResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class TradeService(
    private val bokslStockProperties: BokslStockProperties,
    private val stockRestTemplate: RestTemplate,
) {
    fun getToken(): TokenResponse {
        val koreainvestment = bokslStockProperties.koreainvestment
        val url = koreainvestment.trade.url + "/oauth2/tokenP"
        val tokenRequest = TokenRequest(
            appkey = koreainvestment.appkey,
            appsecret = koreainvestment.appsecret,
            grantType = "client_credentials"
        )
        val httpEntity = HttpEntity<TokenRequest>(tokenRequest)

        val result = stockRestTemplate.exchange(url, HttpMethod.POST, httpEntity, TokenResponse::class.java)

        return result.body ?: throw RuntimeException("API 결과 없음")
    }

    fun getHashKey(body: Any): String {
        val koreainvestment = bokslStockProperties.koreainvestment
        val url = koreainvestment.trade.url + "/uapi/hashkey"
        val headers = HttpHeaders()
        headers.set("appkey", bokslStockProperties.koreainvestment.appkey)
        headers.set("appsecret", bokslStockProperties.koreainvestment.appsecret)

        val httpEntity = HttpEntity<Any>(body, headers)
        val result = stockRestTemplate.exchange(url, HttpMethod.POST, httpEntity, HashResponse::class.java)
        return result.body?.hash ?: throw RuntimeException("API 결과 없음")
    }
}