package com.setvect.bokslstock2.crawl.dart.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 상세 제무 제표
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

    @JsonProperty("stock_code")
    val stockCode: String,  // 상장회사의 종목코드(6자리)

    @JsonProperty("fs_div")
    val fsDiv: String,  // CFS:연결재무제표, OFS:재무제표

    @JsonProperty("fs_nm")
    val fsNm: String,  // 연결재무제표 또는 재무제표 출력

    @JsonProperty("sj_div")
    val sjDiv: String,  // BS:재무상태표, IS:손익계산서

    @JsonProperty("sj_nm")
    val sjNm: String,  // 재무제표명(재무상태표 또는 손익계산서 출력)

    @JsonProperty("account_nm")
    val accountNm: String,  // 계정명(유동자산, 비유동자산)

    @JsonProperty("thstrm_nm")
    val thstrmNm: String,  // 당기명


    @JsonProperty("thstrm_dt")
    val thstrmDt: String,  // 당기일자, ex) 2018.09.30 현재

    @JsonProperty("thstrm_amount")
    val thstrmAmount: String,  // 당기금액

    @JsonProperty("thstrm_add_amount")
    val thstrmAddAmount: String?,  // 당기누적금액

    @JsonProperty("frmtrm_nm")
    val frmtrmNm: String?,  // 전기명, ex) 제 12 기말

    @JsonProperty("frmtrm_dt")
    val frmtrmDt: String?,  // 전기일자

    @JsonProperty("frmtrm_amount")
    val frmtrmAmount: String?,  // 전기금액

    // TODO 있는지 확인
    @JsonProperty("frmtrm_q_nm")
    val frmtrmQNm: String?,  // 전기명(분/반기)

    // TODO 있는지 확인
    @JsonProperty("frmtrm_q_amount")
    val frmtrmQAmount: String?,  // 전기금액(분/반기)

    @JsonProperty("frmtrm_add_amount")
    val frmtrmAddAmount: String?,  // 전기누적금액

    // TODO 있는지 확인
    @JsonProperty("bfefrmtrm_nm")
    val bfefrmtrmNm: String?,  // 전전기명

    // TODO 있는지 확인
    @JsonProperty("bfefrmtrm_amount")
    val bfefrmtrmAmount: String?,  // 전전기금액

    val ord: String,  // 계정과목 정렬순서

    val currency: String  // 통화 단위
)
