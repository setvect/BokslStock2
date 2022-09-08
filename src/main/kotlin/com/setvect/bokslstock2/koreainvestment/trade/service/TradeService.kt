package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.model.BaseHeader
import com.setvect.bokslstock2.koreainvestment.trade.model.request.CurrentPriceRequest
import com.setvect.bokslstock2.koreainvestment.trade.model.request.DatePriceRequest
import com.setvect.bokslstock2.koreainvestment.trade.model.request.TokenRequest
import com.setvect.bokslstock2.koreainvestment.trade.model.response.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class TradeService(
    private val bokslStockProperties: BokslStockProperties,
    private val stockRestTemplate: RestTemplate,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

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

        val headers = BaseHeader(
            appkey = bokslStockProperties.koreainvestment.appkey,
            appsecret = bokslStockProperties.koreainvestment.appsecret
        ).headers()

        val httpEntity = HttpEntity<Any>(body, headers)
        val result = stockRestTemplate.exchange(url, HttpMethod.POST, httpEntity, HashResponse::class.java)
        return result.body?.hash ?: throw RuntimeException("API 결과 없음")
    }

    /**
     * @return 현재 가격
     */
    fun getCurrentPrice(request: CurrentPriceRequest, authorization: String): CommonResponse<CurrentPriceResponse> {
        val koreainvestment = bokslStockProperties.koreainvestment
        val url = koreainvestment.trade.url + "/uapi/domestic-stock/v1/quotations/inquire-price?fid_cond_mrkt_div_code=J&fid_input_iscd={code}"

        val headers = BaseHeader(
            appkey = bokslStockProperties.koreainvestment.appkey,
            appsecret = bokslStockProperties.koreainvestment.appsecret,
            authorization = authorization,
            trId = request.wsTransaction.trId
        ).headers()

        val httpEntity = HttpEntity<Void>(headers)

        val result = stockRestTemplate.exchange(
            url,
            HttpMethod.GET,
            httpEntity,
            object : ParameterizedTypeReference<CommonResponse<CurrentPriceResponse>>() {},
            request.code
        )
        return result.body ?: throw RuntimeException("API 결과 없음")
    }

    fun getDatePrice(request: DatePriceRequest, authorization: String): CommonResponse<List<DatePriceResponse>> {
        val koreainvestment = bokslStockProperties.koreainvestment
        val url = koreainvestment.trade.url + "/uapi/domestic-stock/v1/quotations/inquire-daily-price?fid_cond_mrkt_div_code=J&fid_input_iscd={code}&fid_period_div_code={dateType}&fid_org_adj_prc=0"

        val headers = BaseHeader(
            appkey = bokslStockProperties.koreainvestment.appkey,
            appsecret = bokslStockProperties.koreainvestment.appsecret,
            authorization = authorization,
            trId = request.wsTransaction.trId
        ).headers()

        val httpEntity = HttpEntity<Void>(headers)

        val result = stockRestTemplate.exchange(
            url,
            HttpMethod.GET,
            httpEntity,
            object : ParameterizedTypeReference<CommonResponse<List<DatePriceResponse>>>() {},
            request.code,
            request.dateType.value
        )
        return result.body ?: throw RuntimeException("API 결과 없음")

    }


}