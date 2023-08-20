package com.setvect.bokslstock2.crawl.dart.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 상세 재무제표
 * 참고로 ResFinancialStatement 는 주요 재무제표
 * ResFinancialStatement 항목이 더 많지만 본 응담 모델의 계정명이 더 많다(설명이 어렵네)
 */
data class ResFinancialDetailStatement(
    @JsonProperty("rcept_no")
    val rceptNo: String,   // 접수번호(14자리)

    @JsonProperty("reprt_code")
    val reprtCode: String,  // 보고서 코드 1분기보고서 : 11013 반기보고서 : 11012 3분기보고서 : 11014 사업보고서 : 11011

    @JsonProperty("bsns_year")
    val bsnsYear: String,  // 사업연도(4자리)

    @JsonProperty("corp_code")
    val corpCode: String,  // 상장회사의 종목코드(6자리)

    var stockCode: String?,  // 상장회사의 종목코드(6자리), 응답 없음 . 수동으로 채워넣기

    var fsDiv: String?,  // CFS:연결재무제표, OFS:재무제표, 응답 없음. 수동으로 채워넣기

    @JsonProperty("sj_div")
    val sjDiv: String,  // BS : 재무상태표 IS : 손익계산서 CIS : 포괄손익계산서 CF : 현금흐름표 SCE : 자본변동표

    @JsonProperty("sj_nm")
    val sjNm: String,  // 재무제표명(재무상태표 또는 손익계산서 출력)

    @JsonProperty("account_id")
    val accountId: String,  // 계정ID

    @JsonProperty("account_nm")
    val accountNm: String,  // 계정명(유동자산, 비유동자산)

    @JsonProperty("account_detail")
    val accountDetail: String,  // 계정상세

    @JsonProperty("thstrm_nm")
    val thstrmNm: String,  // 당기명

    @JsonProperty("thstrm_amount")
    val thstrmAmount: String,  // 당기금액

    @JsonProperty("thstrm_add_amount")
    val thstrmAddAmount: String?,  // 당기누적금액

    @JsonProperty("frmtrm_nm")
    val frmtrmNm: String?,  // 전기명, ex) 제 12 기말

    @JsonProperty("frmtrm_amount")
    val frmtrmAmount: String?,  // 전기금액

    @JsonProperty("frmtrm_q_nm")
    val frmtrmQNm: String?,  // 전기명(분/반기), 있는 데이터는 모두 null 되어 있음

    @JsonProperty("frmtrm_q_amount")
    val frmtrmQAmount: String?,  // 전기금액(분/반기), 있는 데이터는 모두 null 되어 있음

    @JsonProperty("frmtrm_add_amount")
    val frmtrmAddAmount: String?,  // 전기누적금액

    @JsonProperty("bfefrmtrm_nm")
    val bfefrmtrmNm: String?,  // 전전기명

    @JsonProperty("bfefrmtrm_amount")
    val bfefrmtrmAmount: String?,  // 전전기금액, 있는 데이터는 모두 null 되어 있음

    val ord: String,  // 계정과목 정렬순서

    val currency: String  // 통화 단위
)
