package com.setvect.bokslstock2.koreainvestment.ws.model

import org.apache.commons.lang3.StringUtils

/**
 * [dataType] 0 또는 1이면 실시간 데이터?,  정확히 뭔지 모름
 * [trId] 업무 종류 [WsTransaction] 참고
 * [dataCount] 체결데이터 개수
 * [responseData] 각 업무에 맞는 응답 데이터
 */
data class WsResponse(val dataType: String, val trId: String, val dataCount: Int, val responseData: String) {
    companion object {

        /**
         * 예시값
         * [rawText] 0|H0STASP0|001|233740^152809^A^8960^8965^8970^0 ... 생략 ...
         */
        fun parsing(rawText: String): WsResponse {
            val dataArray = StringUtils.split(rawText, "|")

            return WsResponse(
                dataArray[0], dataArray[1], dataArray[2].toInt(), dataArray[3]
            )
        }
    }
}