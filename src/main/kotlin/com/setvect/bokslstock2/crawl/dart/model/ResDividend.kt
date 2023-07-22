package com.setvect.bokslstock2.crawl.dart.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ResDividend(
    @JsonProperty("rcept_no")
    val rceptNo: String, // 접수번호(14자리)

    @JsonProperty("corp_cls")
    val corpCls: String, // 법인구분 : Y(유가), K(코스닥), N(코넥스), E(기타)

    @JsonProperty("corp_code")
    val corpCode: String, // 공시대상회사의 고유번호(8자리)

    @JsonProperty("corp_name")
    val corpName: String, // 법인명

    @JsonProperty("se")
    val se: String, // 유상증자(주주배정), 전환권행사 등

    @JsonProperty("stock_knd")
    val stockKnd: String?, // 주식 종류 : 보통주, 우선주

    @JsonProperty("thstrm")
    val thstrm: String, // 당기, 9,999,999,999

    @JsonProperty("frmtrm")
    val frmtrm: String, // 전기, 9,999,999,999

    @JsonProperty("lwfr")
    val lwfr: String // 전전기, 9,999,999,999
)