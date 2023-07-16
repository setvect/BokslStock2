package com.setvect.bokslstock2.crawl.dart.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 주식의 총수
 */
data class ResStockQuantity(
    @JsonProperty("rcept_no") 
    // 접수번호(14자리)
    val rceptNo: String,

    @JsonProperty("corp_cls") 
    // 법인구분 : Y(유가), K(코스닥), N(코넥스), E(기타)
    val corpCls: String,

    @JsonProperty("corp_code") 
    // 공시대상회사의 고유번호(8자리)
    val corpCode: String,

    @JsonProperty("corp_name") 
    // 공시대상회사명
    val corpName: String,

    @JsonProperty("se") 
    // 구분(증권의종류, 합계, 비고)
    val se: String,

    @JsonProperty("isu_stock_totqy") 
    // Ⅰ. 발행할 주식의 총수, 9,999,999,999
    val isuStockTotqy: String,

    @JsonProperty("now_to_isu_stock_totqy") 
    // Ⅱ. 현재까지 발행한 주식의 총수, 9,999,999,999
    val nowToIsuStockTotqy: String,

    @JsonProperty("now_to_dcrs_stock_totqy") 
    // Ⅲ. 현재까지 감소한 주식의 총수, 9,999,999,999
    val nowToDcrsStockTotqy: String,

    @JsonProperty("redc") 
    // Ⅲ. 현재까지 감소한 주식의 총수(1. 감자), 9,999,999,999
    val redc: String,

    @JsonProperty("profit_incnr") 
    // Ⅲ. 현재까지 감소한 주식의 총수(2. 이익소각), 9,999,999,999
    val profitIncnr: String,

    @JsonProperty("rdmstk_repy") 
    // Ⅲ. 현재까지 감소한 주식의 총수(3. 상환주식의 상환), 9,999,999,999
    val rdmstkRepy: String,

    @JsonProperty("etc") 
    // Ⅲ. 현재까지 감소한 주식의 총수(4. 기타), 9,999,999,999
    val etc: String,

    @JsonProperty("istc_totqy") 
    // Ⅳ. 발행주식의 총수 (Ⅱ-Ⅲ), 9,999,999,999
    val istcTotqy: String,

    @JsonProperty("tesstk_co") 
    // Ⅴ. 자기주식수, 9,999,999,999
    val tesstkCo: String,

    @JsonProperty("distb_stock_co") 
    // Ⅵ. 유통주식수 (Ⅳ-Ⅴ), 9,999,999,999
    val distbStockCo: String
)