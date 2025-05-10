package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.model.BaseHeader
import com.setvect.bokslstock2.koreainvestment.trade.model.request.*
import com.setvect.bokslstock2.koreainvestment.trade.model.response.*
import com.setvect.bokslstock2.koreainvestment.ws.model.StockTransaction
import com.setvect.bokslstock2.slack.SlackMessageService
import com.setvect.bokslstock2.util.DateUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import kotlin.math.pow

@Service
class StockClientService(
    private val bokslStockProperties: BokslStockProperties,
    private val stockRestTemplate: RestTemplate,
    private val slackMessageService: SlackMessageService,
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
    // API 문서(https://apiportal.koreainvestment.com/apiservice/oauth2)를 보면 hashkey값이 필수가 아니라고 되어 있음
    @Deprecated("사용안함")
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

        val headers = headerAuth(authorization, request.stockTransaction)

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
     * TODO 아래 코멘트 맞는지 검증
     * 장 시작 동시호가 시간에 호출 하면 오늘 날짜도 포함해서 반환.
     * 그러니깐 0번째는 오늘, 1번째는 전일 가격이다.
     * 만약 휴장일에 호출하면 0번째는 마지막 영업일이다.
     * @return 일, 주, 월 시세 최근 날짜 순으로 30개 반환
     */
    fun requestDatePrice(request: DatePriceRequest, authorization: String): CommonResponse<List<DatePriceResponse>> {
        val url = bokslStockProperties.koreainvestment.trade.url +
                "/uapi/domestic-stock/v1/quotations/inquire-daily-price?" +
                "fid_cond_mrkt_div_code=J&fid_input_iscd={code}&fid_period_div_code={dateType}&fid_org_adj_prc=0"

        val headers = headerAuth(authorization, request.stockTransaction)

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
     * @return 1분봉 제공
     */
    fun requestMinutePrice(request: MinutePriceRequest, authorization: String): MinutePriceResponse {
        val url = bokslStockProperties.koreainvestment.trade.url +
                "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice?" +
                "fid_cond_mrkt_div_code=J&fid_etc_cls_code=&fid_input_hour_1={time}&fid_input_iscd={code}&fid_pw_data_incu_yn=Y"

        val headers = headerAuth(authorization, request.stockTransaction)

        val httpEntity = HttpEntity<Void>(headers)

        val result = stockRestTemplate.exchange(
            url,
            HttpMethod.GET,
            httpEntity,
            MinutePriceResponse::class.java,
            mapOf(
                "code" to request.code,
                "time" to DateUtil.format(request.time, "HHmmss")
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

        val headers = headerAuth(authorization, request.stockTransaction)

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
     * @return 휴장일 조회
     */
    fun requestHoliday(request: HolidayRequest, authorization: String): CommonResponse<List<HolidayResponse>> {
        val url = bokslStockProperties.koreainvestment.trade.url +
                "/uapi/domestic-stock/v1/quotations/chk-holiday?BASS_DT={date}&CTX_AREA_NK=&CTX_AREA_FK="

        val headers = headerAuth(authorization, request.stockTransaction)

        val httpEntity = HttpEntity<Void>(headers)

        val result = stockRestTemplate.exchange(
            url,
            HttpMethod.GET,
            httpEntity,
            object : ParameterizedTypeReference<CommonResponse<List<HolidayResponse>>>() {},
            mapOf("date" to DateUtil.format(request.baseDate, "yyyyMMdd"))
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

        val headers = headerAuth(authorization, request.stockTransaction)

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
     * @return 주식정정 취소 가능 주문 조회
     */
    fun requestCancelableList(
        request: CancelableRequest,
        authorization: String
    ): CommonResponse<List<CancelableResponse>> {
        val url = bokslStockProperties.koreainvestment.trade.url +
                "/uapi/domestic-stock/v1/trading/inquire-psbl-rvsecncl?" +
                "CANO={cano}&ACNT_PRDT_CD=01&CTX_AREA_FK100=&CTX_AREA_NK100=&INQR_DVSN_1=1&INQR_DVSN_2=0"

        val headers = headerAuth(authorization, request.stockTransaction)

        val httpEntity = HttpEntity<Void>(headers)

        val result = stockRestTemplate.exchange(
            url,
            HttpMethod.GET,
            httpEntity,
            object : ParameterizedTypeReference<CommonResponse<List<CancelableResponse>>>() {},
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
    fun requestOrderSell(request: OrderRequest, authorization: String): CommonResponse<OrderResponse> {
        return order(request, authorization, request.sell)
    }

    private fun order(
        request: OrderRequest,
        authorization: String,
        order: StockTransaction
    ): CommonResponse<OrderResponse> {
        val url = bokslStockProperties.koreainvestment.trade.url + "/uapi/domestic-stock/v1/trading/order-cash"
        val headers = headerAuthHash(authorization, order)
        val httpEntity = HttpEntity<OrderRequest>(request, headers)

        val maxRetries = 5           // 최대 재시도 횟수
        var attempt = 0              // 현재 시도 횟수
        var lastException: Exception? = null

        while (attempt < maxRetries) {
            try {
                log.info("주문 요청 시도 중 (${attempt + 1}/${maxRetries}): ${request.code}")
                val result = stockRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpEntity,
                    object : ParameterizedTypeReference<CommonResponse<OrderResponse>>() {}
                )
                log.info("주문 요청 성공: ${request.code}")
                return result.body ?: throw RuntimeException("API 결과 없음")
            } catch (e: Exception) {
                lastException = e
                attempt++
                val message = "주문 요청 실패 (${attempt}/${maxRetries}): ${e.javaClass.simpleName} - ${e.message}"
                log.warn(message)
                slackMessageService.sendMessage(message)

                if (attempt < maxRetries) {
                    val waitTime = calculateWaitTime(attempt)
                    log.info("${waitTime}ms 후 재시도합니다.")
                    Thread.sleep(waitTime)
                }
            }
        }

        // 모든 재시도가 실패한 경우 마지막 예외를 던집니다
        log.error("최대 재시도 횟수(${maxRetries})를 초과했습니다. 주문 요청 실패: ${request.code}")
        throw lastException ?: RuntimeException("API 호출 실패", lastException)
    }

    // 지수적 백오프를 사용한 대기 시간 계산
    private fun calculateWaitTime(attempt: Int): Long {
        val baseWaitTimeMs = 1000L   // 기본 대기 시간 (1초)
        val maxWaitTimeMs = 5000L   // 최대 대기 시간 (5초)

        // 지수적 백오프: 2^attempt * baseWaitTimeMs (최대값 제한)
        val waitTime = (2.0.pow(attempt.toDouble()) * baseWaitTimeMs).toLong()
        return waitTime.coerceAtMost(maxWaitTimeMs)
    }

    private fun headerAuth(authorization: String, stockTransaction: StockTransaction): HttpHeaders {
        return BaseHeader(
            appkey = bokslStockProperties.koreainvestment.appkey,
            appsecret = bokslStockProperties.koreainvestment.appsecret,
            authorization = authorization,
            trId = stockTransaction.trId
        ).headers()
    }

    private fun headerAuthHash(
        authorization: String,
        stockTransaction: StockTransaction,
    ): HttpHeaders {
        return BaseHeader(
            appkey = bokslStockProperties.koreainvestment.appkey,
            appsecret = bokslStockProperties.koreainvestment.appsecret,
            authorization = authorization,
            trId = stockTransaction.trId,
        ).headers()
    }

    // TODO 체결내역 작업해야됨


}
