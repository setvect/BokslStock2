package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.model.BaseHeader
import com.setvect.bokslstock2.koreainvestment.trade.model.request.*
import com.setvect.bokslstock2.koreainvestment.trade.model.response.*
import com.setvect.bokslstock2.koreainvestment.ws.model.StockTransaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class StockClientService(
    private val bokslStockProperties: BokslStockProperties,
    private val stockRestTemplate: RestTemplate,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * @return access token
     */
    fun requestToken(): TokenResponse {
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

    /**
     * @return 주문에 사용되는 post API 사용시 쓰이는 hashkey
     */
    fun requestHashKey(body: Any): String {
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
    fun requestCurrentPrice(request: CurrentPriceRequest, authorization: String): CommonResponse<CurrentPriceResponse> {
        val url = bokslStockProperties.koreainvestment.trade.url +
            "/uapi/domestic-stock/v1/quotations/inquire-price?fid_cond_mrkt_div_code=J&fid_input_iscd={code}"

        val headers = headerAuth(authorization, request.wsTransaction)

        val httpEntity = HttpEntity<Void>(headers)

        val result = stockRestTemplate.exchange(
            url,
            HttpMethod.GET,
            httpEntity,
            object : ParameterizedTypeReference<CommonResponse<CurrentPriceResponse>>() {},
            mapOf("code" to request.code)
        )
        return result.body ?: throw RuntimeException("API 결과 없음")
    }

    /**
     * @return 일, 주, 월 시세 최근 날짜 순으로 30개 반환
     */
    fun requestDatePrice(request: DatePriceRequest, authorization: String): CommonResponse<List<DatePriceResponse>> {
        val url = bokslStockProperties.koreainvestment.trade.url +
            "/uapi/domestic-stock/v1/quotations/inquire-daily-price?" +
            "fid_cond_mrkt_div_code=J&fid_input_iscd={code}&fid_period_div_code={dateType}&fid_org_adj_prc=0"

        val headers = headerAuth(authorization, request.wsTransaction)

        val httpEntity = HttpEntity<Void>(headers)

        val result = stockRestTemplate.exchange(
            url,
            HttpMethod.GET,
            httpEntity,
            object : ParameterizedTypeReference<CommonResponse<List<DatePriceResponse>>>() {},
            mapOf(
                "code" to request.code,
                "dateType" to request.dateType.value
            )
        )
        return result.body ?: throw RuntimeException("API 결과 없음")
    }

    /**
     * @return 호가 조회
     */
    fun requestQuote(request: QuoteRequest, authorization: String): QuoteResponse {
        val url = bokslStockProperties.koreainvestment.trade.url +
            "/uapi/domestic-stock/v1/quotations/inquire-asking-price-exp-ccn?fid_cond_mrkt_div_code=J&fid_input_iscd={code}"

        val headers = headerAuth(authorization, request.wsTransaction)

        val httpEntity = HttpEntity<Void>(headers)

        val result = stockRestTemplate.exchange(
            url,
            HttpMethod.GET,
            httpEntity,
            QuoteResponse::class.java,
            mapOf("code" to request.code)
        )
        return result.body ?: throw RuntimeException("API 결과 없음")
    }

    /**
     * @return 주식, 예수금
     */
    fun requestBalance(request: BalanceRequest, authorization: String): BalanceResponse {
        val url = bokslStockProperties.koreainvestment.trade.url +
            "/uapi/domestic-stock/v1/trading/inquire-balance?" +
            "CANO={cano}&ACNT_PRDT_CD=01&AFHR_FLPR_YN=N&OFL_YN=&INQR_DVSN=01&UNPR_DVSN=01&" +
            "FUND_STTL_ICLD_YN=N&FNCG_AMT_AUTO_RDPT_YN=N&PRCS_DVSN=00&CTX_AREA_FK100=null&CTX_AREA_NK100="

        val headers = headerAuth(authorization, request.wsTransaction)

        val httpEntity = HttpEntity<Void>(headers)

        val result = stockRestTemplate.exchange(
            url,
            HttpMethod.GET,
            httpEntity,
            BalanceResponse::class.java,
            mapOf<String, String>("cano" to request.accountNo)
        )
        return result.body ?: throw RuntimeException("API 결과 없음")
    }

    /**
     * 매수 주문
     */
    fun requestOrderBuy(request: OrderRequest, authorization: String): CommonResponse<OrderResponse> {
        return order(request, authorization, request.buy)
    }

    /**
     * 매도 주문
     */
    fun requestOrderSell(request: OrderRequest, authorization: String): Any {
        return order(request, authorization, request.sell)
    }

    private fun order(
        request: OrderRequest,
        authorization: String,
        order: StockTransaction
    ): CommonResponse<OrderResponse> {
        val url = bokslStockProperties.koreainvestment.trade.url + "/uapi/domestic-stock/v1/trading/order-cash"

        val hashKey = this.requestHashKey(request)
        val headers = headerAuthHash(authorization, order, hashKey)

        val httpEntity = HttpEntity<OrderRequest>(request, headers)

        val result = stockRestTemplate.exchange(
            url,
            HttpMethod.POST,
            httpEntity,
            object : ParameterizedTypeReference<CommonResponse<OrderResponse>>() {},
        )
        return result.body ?: throw RuntimeException("API 결과 없음")
    }

    private fun headerAuth(authorization: String, stockTransaction: StockTransaction): HttpHeaders {
        return BaseHeader(
            appkey = bokslStockProperties.koreainvestment.appkey,
            appsecret = bokslStockProperties.koreainvestment.appsecret,
            authorization = authorization,
            trId = stockTransaction.trId
        ).headers()
    }

    private fun headerAuthHash(authorization: String, stockTransaction: StockTransaction, hashKey: String): HttpHeaders {
        return BaseHeader(
            appkey = bokslStockProperties.koreainvestment.appkey,
            appsecret = bokslStockProperties.koreainvestment.appsecret,
            authorization = authorization,
            trId = stockTransaction.trId,
            hashKey = hashKey
        ).headers()
    }

    // TODO 체결내역 작업해야됨


}